package com.bank.payementService.Service;

import com.bank.payementService.DTO.CreatePaymentRequest;
import com.bank.payementService.DTO.PaymentOrderResponse;
import com.bank.payementService.Entity.Payment;
import com.bank.payementService.Entity.PaymentStatus;
import com.bank.payementService.Repo.PaymentRepository;
import jakarta.persistence.criteria.Order;
import jakarta.validation.Valid;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.util.JSONPObject;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String,Object> kafkaTemplate;

    @Value("${razorpay.key-id}")
    private String keyId;


    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String PAYMENT_COMPLETED_TPOIC="payment.completed";
    private static final String PAYMENT_FAILED_TPOIC="payment.failed";



    /**
     * Create Razorpay pay order
     *
     * FLOW:
     * 1.create order in razorpay
     * 2.save payment record in db
     * 3.return order details in frontend
     * 4.frontend show razorpay checkout
     * 5.user pays
     * 6.razorpay calls webhook
     * @param request
     * @return
     */

    public PaymentOrderResponse createPaymentOrder(@Valid CreatePaymentRequest request) throws RazorpayException {
        log.info("Creating payment order for account:{} amount:{}",
                request.getAccountNumber(),request.getAmount());

        RazorpayClient razorpayClient=new RazorpayClient(keyId,keySecret);

        int convertedAmount=request.getAmount().multiply(BigDecimal.valueOf(100)).intValue();

        JSONPObject orderRequest=new JSONPObject();
        orderRequest.put("amount",convertedAmount);
        orderRequest.put("currency","USD/INR");
        orderRequest.put("receipt","rcpt_"+System.currentTimeMillis()+ UUID.randomUUID().toString().replace("-","").substring(0,10));

        Order razorpayOrder=razorpayClient.orders.create(orderRequest);

        log.info("Razorpay order created.{}",razorpayOrder.get("id").toString());

        Payment payment=new Payment();
        payment.setRazorpayOrderId(razorpayOrder.get("id").toString());
        payment.setAccountNumber(request.getAccountNumber());
        payment.setAmount(request.getAmount());
        payment.setCurrency("USD/INR");
        payment.setStatus(PaymentStatus.CREATED);
        payment.setDescription(request.getDescription());


        Payment savedPayment=paymentRepository.save(payment);

        return new PaymentOrderResponse(
                savedPayment.getId(),
                razorpayOrder.get("id").toString,
                request.getAmount(),
                "USD/INR",
                "CREATED",
                keyId

        );
    }
    public void handleWebHook(Map<String, Object> payload) {


        log.info("Received Razorpay Webhook:{}",payload.get("event"));

        String event=(String)payload.get("event");

        if("payment.captured".equals(event)){
            handlePaymentSuccess(payload);
        }else if("payment.failed".equals(event)){
            handlePaymentFailed(payload);
        }
    }

    private void handlePaymentSuccess(Map<String,Object>payload){

        try{

            Map<String,Object> paymentData=extractPaymentData(payload);
            String orderId=(String)paymentData.get("order_id");
            String paymentId=(String)paymentData.get("payment_id");


            Payment payment=paymentRepository.findByRazorpayOrderId(orderId)
                    .orElseThrow(()-> new RuntimeException
                            ("Payment not found for orderId:"+orderId));

            payment.setRazorpayPaymentId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);

            //published payment event completed

            Map<String,Object>event=new HashMap<>();
            event.put("paymentId",payment.getId());
            event.put("accountNumber",payment.getAccountNumber());
            event.put("amount",payment.getAmount());
            event.put("razorpayPaymentId",paymentId);


            kafkaTemplate.send(PAYMENT_COMPLETED_TPOIC,payment.getId(),event);

            log.info("Payment completed:{}",payment.getId());
        }catch(Exception e){
            log.error("Error handling payment success:{} ",e.getMessage());

        }

    }

    private void handlePaymentFailed(Map<String,Object>payload){

    }
}
