package com.hotel.erp;

import org.springframework.boot.SpringApplication;

public class TestHotelErpApplication {

	public static void main(String[] args) {
		SpringApplication.from(HotelErpApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
