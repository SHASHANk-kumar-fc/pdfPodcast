package com.pdfpodcast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableCaching
@EnableRetry
@EnableScheduling
public class PdfpodcastApplication {

	public static void main(String[] args) {
		SpringApplication.run(PdfpodcastApplication.class, args);
	}

}
