package io.github.zam0k.simplifiedpsp.services.impl;

import io.github.zam0k.simplifiedpsp.controllers.dto.TransactionDTO;
import io.github.zam0k.simplifiedpsp.domain.IPayee;
import io.github.zam0k.simplifiedpsp.domain.IPayer;
import io.github.zam0k.simplifiedpsp.domain.Transaction;
import io.github.zam0k.simplifiedpsp.repositories.JuridicalPersonRepository;
import io.github.zam0k.simplifiedpsp.repositories.NaturalPersonRepository;
import io.github.zam0k.simplifiedpsp.repositories.TransactionRepository;
import io.github.zam0k.simplifiedpsp.services.TransactionService;
import io.github.zam0k.simplifiedpsp.services.exceptions.BadGatewayException;
import io.github.zam0k.simplifiedpsp.services.exceptions.BadRequestException;
import io.github.zam0k.simplifiedpsp.services.exceptions.NotFoundException;
import io.github.zam0k.simplifiedpsp.utils.PaymentNotifier;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.HttpStatus.OK;

@Service
@RequiredArgsConstructor
@Log4j2
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final NaturalPersonRepository naturalPersonRepository;
    private final JuridicalPersonRepository juridicalPersonRepository;
    private final ModelMapper mapper;
    private final RestTemplate restTemplate;

    @Autowired
    private PaymentNotifier notifier;

    @Override
    public Transaction create(TransactionDTO entity) {

        Transaction transaction = mapper.map(entity, Transaction.class);
        BigDecimal value = transaction.getValue();
        IPayer payer = getPayer(transaction);

        if(value.compareTo(payer.getBalance()) >= 0)
            throw new BadRequestException("Insufficient funds");

        IPayee payee = getPayee(transaction);

        return executeTransaction(transaction, value, payer, payee);
    }

    @Transactional
    private Transaction executeTransaction(Transaction transaction, BigDecimal value, IPayer payer, IPayee payee) {
        payee.receiveValue(value);
        payer.removeValue(value);

        authorizeTransaction();

        // TO-DO: find a way to optimize this so it doesn't take 8 seconds to complete the transaction
        this.notifyPayee(payee);

        return repository.save(transaction);
    }

    private void notifyPayee(IPayee payee) {
        notifier.notifyPayee(payee);
    }

    private void authorizeTransaction() {
        String externalAuthorizerServiceURL =
                "https://run.mocky.io/v3/8fafdd68-a090-496f-8c9a-3442cf30dae6";

        ResponseEntity<String> response;

        try {
            response = restTemplate
                    .getForEntity(externalAuthorizerServiceURL, String.class);
        } catch (RestClientException e) {
            throw new BadGatewayException("External authorizer service currently unavailable");
        }

        // TO-DO: check if there's a more fitting error for this
        if(response.getStatusCode() != OK) throw new BadRequestException("Transaction rejected");
    }

    private IPayee getPayee(Transaction transaction) {
        Long payeeId = transaction.getPayee();
        return Stream.of(naturalPersonRepository.findById(payeeId), juridicalPersonRepository.findById(payeeId))
                .filter(Optional::isPresent).map(Optional::get).findFirst()
                .orElseThrow(NotFoundException::new);

    }

    private IPayer getPayer(Transaction transaction) {
        Long payerId = transaction.getPayer();
        return naturalPersonRepository.findById(payerId).orElseThrow(NotFoundException::new);
    }
}
