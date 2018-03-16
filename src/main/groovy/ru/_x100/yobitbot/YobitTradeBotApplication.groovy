package ru._x100.yobitbot

import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class YobitTradeBotApplication {

	static void main(String[] args) {
		SpringApplication.run YobitTradeBotApplication, args
	}
}
