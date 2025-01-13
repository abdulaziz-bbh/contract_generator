package com.example.contract_generator

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableJpaRepositories(repositoryBaseClass = BaseRepositoryImpl::class)
@EnableScheduling
class ContractGeneratorApplication

fun main(args: Array<String>) {
    runApplication<ContractGeneratorApplication>(*args)
}
