package br.com.zelo.platform.beneficiary;

public record DocumentContent(String fileName, String contentType, byte[] bytes) {
}
