package com.picpaysimplificado.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.picpaysimplificado.domain.transaction.Transaction;
import com.picpaysimplificado.domain.user.User;
import com.picpaysimplificado.dtos.TransactionDTO;
import com.picpaysimplificado.repositories.TransactionRepository;

@Service
public class TransactionService {
	
	@Autowired
	private UserService userService;
	
	@Autowired
	private NotificationService notificationService;
	
	@Autowired
	private TransactionRepository transactionRepository;
	
	@Autowired
	private RestTemplate restTemplate;
	
	public Transaction createTransaction(TransactionDTO transaction) throws Exception {
		
		User sender = this.userService.findById(transaction.senderId());
		User receiver = this.userService.findById(transaction.receiverId());
		
		userService.validateTransaction(sender, transaction.value());
		
		if(!authorizeTransaction(sender, transaction.value())) {
			throw new Exception("Transação não autorizada!");
		}
		
		Transaction newTransaction = new Transaction();
		newTransaction.setAmount(transaction.value());
		newTransaction.setSender(sender);
		newTransaction.setReceiver(receiver);
		newTransaction.setTimestamp(LocalDateTime.now());
		
		sender.setBalance(sender.getBalance().subtract(transaction.value()));
		receiver.setBalance(receiver.getBalance().add(transaction.value()));
		
		transactionRepository.save(newTransaction);
		userService.saveUser(sender);
		userService.saveUser(receiver);	
		
		notificationService.sendNotification(sender, "Transação realizada com sucesso!");
		notificationService.sendNotification(receiver, "Transação recebida com sucesso!");
		
		return newTransaction;
	}
	
	public boolean authorizeTransaction(User sender, BigDecimal value) {
		
		ResponseEntity<Map> authorizationResponse = restTemplate.getForEntity("https://util.devi.tools/api/v2/authorize", Map.class);
		
		if(authorizationResponse.getStatusCode() == HttpStatus.OK) {
			Map<String, Object> body = authorizationResponse.getBody();
			
			if(body != null) {
				Map<String, Object> data = (Map<String, Object>) body.get("data");
				
				if(data != null) {
					Boolean isAuthorized = (Boolean) data.get("authorization");
					return isAuthorized != null && isAuthorized;
				}
			}
		}
		return false;
	}
	
	
	
	
	
}
