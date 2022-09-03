package io.github.zam0k.simplifiedpsp.services.impl;

import io.github.zam0k.simplifiedpsp.controllers.dto.CommonUserDTO;
import io.github.zam0k.simplifiedpsp.domain.CommonUser;
import io.github.zam0k.simplifiedpsp.repositories.CommonUserRepository;
import io.github.zam0k.simplifiedpsp.services.CommonUserService;
import io.github.zam0k.simplifiedpsp.services.exceptions.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommonUserServiceImpl implements CommonUserService {

    private final ModelMapper mapper;

    @Autowired
    private CommonUserRepository repository;

    @Override
    public CommonUser save(CommonUserDTO entity) {
        Optional<CommonUser> cpfAlreadyExists = repository.findByCpf(entity.getCpf());

        if(cpfAlreadyExists.isPresent())
            throw new BadRequestException("Cpf must be unique");

        Optional<CommonUser> emailAlreadyExists = repository.findByEmail(entity.getEmail());

        if (emailAlreadyExists.isPresent())
            throw new BadRequestException("Email must be unique");

        return repository.save(mapper.map(entity, CommonUser.class));
    }
}