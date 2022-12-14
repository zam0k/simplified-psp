package io.github.zam0k.simplifiedpsp.services.impl;

import io.github.zam0k.simplifiedpsp.controllers.dto.TransactionDTO;
import io.github.zam0k.simplifiedpsp.domain.CommonUser;
import io.github.zam0k.simplifiedpsp.domain.Shopkeeper;
import io.github.zam0k.simplifiedpsp.domain.Transaction;
import io.github.zam0k.simplifiedpsp.repositories.CommonUserRepository;
import io.github.zam0k.simplifiedpsp.repositories.ShopkeeperRepository;
import io.github.zam0k.simplifiedpsp.repositories.TransactionRepository;
import io.github.zam0k.simplifiedpsp.services.exceptions.BadRequestException;
import io.github.zam0k.simplifiedpsp.services.exceptions.NotFoundException;
import io.github.zam0k.simplifiedpsp.utils.PaymentNotifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

@ExtendWith(SpringExtension.class)
class TransactionServiceImplTest {
  public static final UUID TRANS_ID = UUID.randomUUID();
  public static final BigDecimal TRANS_BALANCE = BigDecimal.valueOf(10.00);
  public static final LocalDateTime TRANS_TIMESTAMP = LocalDateTime.now();

  public static final UUID PAYER_ID = UUID.randomUUID();
  public static final BigDecimal PAYER_BALANCE = BigDecimal.valueOf(100.00);

  public static final UUID PAYEE_ID = UUID.randomUUID();
  public static final BigDecimal PAYEE_BALANCE = BigDecimal.valueOf(100.00);

  public static final String NOT_FOUND = "Object cannot be found";
  public static final String FUNDS_BAD_REQUEST = "Insufficient funds";
  public static final String TRANSACTION_REJECTED_BAD_REQUEST = "Transaction rejected";
  public static final String SAME_ACCOUNT_BAD_REQUEST = "Can't transfer values to same account";

  @InjectMocks private TransactionServiceImpl service;

  @Mock private CommonUserRepository payerRepository;
  @Mock private ShopkeeperRepository payeeRepository;
  @Mock private ModelMapper mapper;
  @Mock private RestTemplate restTemplate;
  @Mock private PaymentNotifier notifier;
  @Mock private TransactionRepository repository;

  private TransactionDTO transactionDto;
  private Transaction transaction;
  private CommonUser payer;
  private Shopkeeper payee;


  @BeforeEach
  void setUp() {
    initObjects();
  }

  @Test
  void whenSaveTransactionReturnSuccess() {
    when(payerRepository.findById(any())).thenReturn(Optional.of(payer));
    when(payeeRepository.findById(any())).thenReturn(Optional.of(payee));
    when(restTemplate.getForEntity(anyString(), any())).thenReturn(new ResponseEntity<>(OK));
    doNothing().when(notifier).notifyPayee(any(), any());
    when(repository.save(any())).thenReturn(transaction);

    when(mapper.map(any(Transaction.class), any())).thenReturn(transactionDto);

    TransactionDTO response = service.create(transactionDto);

    assertAll(
        () -> assertNotNull(response),
        () -> assertEquals(TransactionDTO.class, response.getClass()),
        () -> assertEquals(TRANS_ID, response.getKey()),
        () -> assertEquals(TRANS_BALANCE, response.getValue()),
        () -> assertEquals(PAYEE_ID, response.getPayee()),
        () -> assertEquals(PAYER_ID, response.getPayer()),
        () -> assertEquals(TRANS_TIMESTAMP, response.getTimestamp()));
  }

  @Test
  void whenSaveTransactionReturnBadRequestInsufficientFunds() {
    transactionDto.setValue(BigDecimal.valueOf(500.00));

    when(payerRepository.findById(any())).thenReturn(Optional.of(payer));
    when(payeeRepository.findById(any())).thenReturn(Optional.of(payee));

    try {
      service.create(transactionDto);
    } catch (Exception ex) {
      assertAll(
          () -> assertEquals(BadRequestException.class, ex.getClass()),
          () -> assertEquals(FUNDS_BAD_REQUEST, ex.getMessage()));
    }
  }

  @Test
  void whenSaveTransactionReturnBadRequestTransactionRejected() {

    when(payerRepository.findById(any())).thenReturn(Optional.of(payer));
    when(payeeRepository.findById(any())).thenReturn(Optional.of(payee));
    when(restTemplate.getForEntity(anyString(), any()))
        .thenReturn(new ResponseEntity<>(BAD_REQUEST));

    try {
      service.create(transactionDto);
    } catch (Exception ex) {
      assertAll(
          () -> assertEquals(BadRequestException.class, ex.getClass()),
          () -> assertEquals(TRANSACTION_REJECTED_BAD_REQUEST, ex.getMessage()));
    }
  }

  @Test
  void whenSaveTransactionReturnNotFoundException() {
    try {
      service.create(transactionDto);
    } catch (Exception ex) {
      assertAll(
          () -> assertEquals(NotFoundException.class, ex.getClass()),
          () -> assertEquals(NOT_FOUND, ex.getMessage()));
    }
  }

  @Test
  void whenSaveTransactionReturnBadRequestException() {
    transactionDto.setPayer(PAYEE_ID);

    try {
      service.create(transactionDto);
    } catch (Exception ex) {
      assertAll(
              () -> assertEquals(BadRequestException.class, ex.getClass()),
              () -> assertEquals(SAME_ACCOUNT_BAD_REQUEST, ex.getMessage()));
    }
  }

  @Test
  void whenFindByIdReturnSuccess() {
    when(repository.findById(TRANS_ID)).thenReturn(Optional.of(transaction));
    when(payeeRepository.findById(PAYEE_ID)).thenReturn(Optional.of(payee));
    when(payerRepository.findById(PAYER_ID)).thenReturn(Optional.of(payer));
    when(mapper.map(any(Transaction.class), any())).thenReturn(transactionDto);

    TransactionDTO response = service.findById(TRANS_ID);

    assertAll(
        () -> assertNotNull(response),
        () -> assertNotNull(response.getLinks()),
        () -> assertTrue(response.hasLink("self")),
        () -> assertEquals(TransactionDTO.class, response.getClass()),
        () -> assertEquals(TRANS_ID, response.getKey()),
        () -> assertEquals(TRANS_BALANCE, response.getValue()),
        () -> assertEquals(PAYEE_ID, response.getPayee()),
        () -> assertEquals(PAYER_ID, response.getPayer()),
        () -> assertEquals(TRANS_TIMESTAMP, response.getTimestamp()));
  }

  @Test
  void whenFindByIdReturnNotFoundException() {

    try {
      service.findById(TRANS_ID);
    } catch (Exception ex) {
      assertAll(
          () -> assertEquals(NotFoundException.class, ex.getClass()),
          () -> assertEquals(NOT_FOUND, ex.getMessage()));
    }
  }

  private void initObjects() {
    transactionDto =
            new TransactionDTO(TRANS_ID, PAYER_ID, PAYEE_ID, TRANS_BALANCE, TRANS_TIMESTAMP);
    transaction = new Transaction(TRANS_ID, PAYER_ID, PAYEE_ID, TRANS_BALANCE, TRANS_TIMESTAMP);
    payer = new CommonUser(PAYER_ID, "", "", "", "", PAYER_BALANCE);
    payee = new Shopkeeper(PAYEE_ID, "", "", "", "", PAYEE_BALANCE);
  }
}
