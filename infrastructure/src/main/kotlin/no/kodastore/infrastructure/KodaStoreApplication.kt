package no.kodastore.infrastructure

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class KodaStoreApplication

fun main(args: Array<String>) {
    runApplication<KodaStoreApplication>(*args)
}
