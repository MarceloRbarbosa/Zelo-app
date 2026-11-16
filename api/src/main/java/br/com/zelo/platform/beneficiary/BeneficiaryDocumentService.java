package br.com.zelo.platform.beneficiary;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import br.com.zelo.platform.shared.exception.InvalidRequestException;
import br.com.zelo.platform.shared.exception.NotFoundException;

@Service
public class BeneficiaryDocumentService {

	private static final List<BeneficiaryStatus> VISIBLE = List.of(BeneficiaryStatus.ACTIVE, BeneficiaryStatus.INACTIVE);

	private final BeneficiaryDocumentRepository repository;

	private final BeneficiaryRepository beneficiaryRepository;

	private final Path directory;

	public BeneficiaryDocumentService(BeneficiaryDocumentRepository repository,
			BeneficiaryRepository beneficiaryRepository, @Value("${zelo.storage.documents-dir}") String directory) {
		this.repository = repository;
		this.beneficiaryRepository = beneficiaryRepository;
		this.directory = Path.of(directory);
	}

	@Transactional(readOnly = true)
	public List<DocumentResponse> list(Long beneficiaryId) {
		requireVisible(beneficiaryId);
		return repository.findAllByBeneficiaryIdOrderByUploadedAtDesc(beneficiaryId)
			.stream()
			.map(DocumentResponse::from)
			.toList();
	}

	// The type comes from the file's first bytes, not from its name or the browser's header.
	@Transactional
	public DocumentResponse upload(Long beneficiaryId, String title, MultipartFile file, Long staffId) {
		requireVisible(beneficiaryId);
		if (title == null || title.isBlank() || title.length() > 100) {
			throw new InvalidRequestException("Title is required and must have at most 100 characters");
		}
		byte[] bytes = read(file);
		String contentType = detectType(bytes);
		if (contentType == null) {
			throw new InvalidRequestException("Only PDF, JPG and PNG files are accepted");
		}
		String key = UUID.randomUUID().toString();
		write(key, bytes);
		afterRollback(() -> delete(key));
		String fileName = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank() ? "documento"
				: Path.of(file.getOriginalFilename()).getFileName().toString();
		BeneficiaryDocument document = repository.save(new BeneficiaryDocument(beneficiaryId, title.trim(),
				fileName.length() > 255 ? fileName.substring(0, 255) : fileName, contentType, bytes.length, key,
				staffId));
		return DocumentResponse.from(document);
	}

	@Transactional(readOnly = true)
	public DocumentContent content(Long beneficiaryId, Long documentId) {
		requireVisible(beneficiaryId);
		BeneficiaryDocument document = find(beneficiaryId, documentId);
		try {
			return new DocumentContent(document.getFileName(), document.getContentType(),
					Files.readAllBytes(directory.resolve(document.getStorageKey())));
		}
		catch (IOException ex) {
			throw new NotFoundException("Document file " + documentId + " is missing");
		}
	}

	@Transactional
	public void remove(Long beneficiaryId, Long documentId) {
		requireVisible(beneficiaryId);
		BeneficiaryDocument document = find(beneficiaryId, documentId);
		repository.delete(document);
		afterCommit(() -> delete(document.getStorageKey()));
	}

	// Used by the LGPD removal: every file of the beneficiary goes away with the personal data.
	@Transactional
	void removeAll(Long beneficiaryId) {
		List<BeneficiaryDocument> documents = repository.findAllByBeneficiaryIdOrderByUploadedAtDesc(beneficiaryId);
		repository.deleteAll(documents);
		afterCommit(() -> documents.forEach(document -> delete(document.getStorageKey())));
	}

	static String detectType(byte[] bytes) {
		if (startsWith(bytes, '%', 'P', 'D', 'F', '-')) {
			return "application/pdf";
		}
		if (startsWith(bytes, 0xFF, 0xD8, 0xFF)) {
			return "image/jpeg";
		}
		if (startsWith(bytes, 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A)) {
			return "image/png";
		}
		return null;
	}

	private static boolean startsWith(byte[] bytes, int... prefix) {
		return bytes.length >= prefix.length && Arrays.equals(Arrays.copyOf(bytes, prefix.length),
				toBytes(prefix));
	}

	private static byte[] toBytes(int[] values) {
		byte[] bytes = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			bytes[i] = (byte) values[i];
		}
		return bytes;
	}

	private void requireVisible(Long beneficiaryId) {
		beneficiaryRepository.findByIdAndStatusIn(beneficiaryId, VISIBLE)
			.orElseThrow(() -> new NotFoundException("Beneficiary " + beneficiaryId + " not found"));
	}

	private BeneficiaryDocument find(Long beneficiaryId, Long documentId) {
		return repository.findByIdAndBeneficiaryId(documentId, beneficiaryId)
			.orElseThrow(() -> new NotFoundException("Document " + documentId + " not found"));
	}

	private static byte[] read(MultipartFile file) {
		try {
			return file.getBytes();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void write(String key, byte[] bytes) {
		try {
			Files.createDirectories(directory);
			Files.write(directory.resolve(key), bytes);
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private void delete(String key) {
		try {
			Files.deleteIfExists(directory.resolve(key));
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	// Files are not transactional: they follow the outcome of the database change.
	private static void afterCommit(Runnable action) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				action.run();
			}
		});
	}

	private static void afterRollback(Runnable action) {
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCompletion(int status) {
				if (status == STATUS_ROLLED_BACK) {
					action.run();
				}
			}
		});
	}

}
