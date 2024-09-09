package com.etr.demo.utils;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public enum LogColors {
	GREEN("\u001B[32m"),
	YELLOW("\u001B[33m"),
	BLUE("\u001B[34m"),
	PURPLE("\u001B[35m"),
	CYAN("\u001B[36m");

	private final String ansi;
	private static final String RESET = "\u001B[0m";

	String paint(String text) {
		return ansi + text + RESET;
	}
}
