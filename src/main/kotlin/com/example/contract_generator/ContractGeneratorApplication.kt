package com.example.contract_generator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
class ContractGeneratorApplication

fun main(args: Array<String>) {
    runApplication<ContractGeneratorApplication>(*args)
}
