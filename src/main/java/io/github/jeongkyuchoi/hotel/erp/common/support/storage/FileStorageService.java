package io.github.jeongkyuchoi.hotel.erp.common.support.storage;

import io.github.jeongkyuchoi.hotel.erp.common.exception.BadRequestException;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 업로드 파일 저장(로컬 볼륨, D-053).
 *
 * <p>업로드된 이미지를 {@code app.upload-dir} 디렉터리에 UUID 파일명으로 저장하고,
 * 부킹엔진·화면에서 쓸 <b>공개 경로</b>({@code /uploads/<파일명>})를 돌려준다. 원본 파일명은
 * 쓰지 않는다 — 경로조작(../)·충돌·인코딩 문제를 원천 차단하기 위함이다.
 *
 * <p><b>왜 서비스로 분리했나</b> — 지금은 로컬 디스크지만, 정식 배포 때 오브젝트
 * 스토리지(Cloudflare R2/S3)로 옮긴다. 저장·삭제를 이 한 곳에 모아 두면 그때 이 클래스의
 * 내부만 바꾸면 되고 호출부(컨트롤러)는 그대로다.
 */
@Slf4j
@Service
public class FileStorageService {

	/** 허용 확장자. 콘텐츠 타입과 함께 이중으로 막는다. */
	private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png", "webp", "gif");
	private static final String PUBLIC_PREFIX = "/uploads/";

	private final Path root;

	public FileStorageService(@Value("${app.upload-dir}") String uploadDir) {
		this.root = Paths.get(uploadDir).toAbsolutePath().normalize();
	}

	@PostConstruct
	void init() {
		try {
			Files.createDirectories(root);
			log.info("업로드 저장 디렉터리: {}", root);
		} catch (IOException e) {
			throw new IllegalStateException("업로드 디렉터리를 만들 수 없습니다: " + root, e);
		}
	}

	/** 실제 서빙에 쓰는 절대 경로(정적 리소스 핸들러가 참조). */
	public Path getRoot() {
		return root;
	}

	/**
	 * 이미지 파일을 저장하고 공개 경로({@code /uploads/xxx.jpg})를 돌려준다.
	 *
	 * @throws BadRequestException 빈 파일·이미지 아님·허용되지 않은 확장자
	 */
	public String store(MultipartFile file) {
		if (file == null || file.isEmpty()) {
			throw new BadRequestException("빈 파일입니다.");
		}
		String contentType = file.getContentType();
		if (contentType == null || !contentType.startsWith("image/")) {
			throw new BadRequestException("이미지 파일만 업로드할 수 있습니다.");
		}
		String ext = extensionOf(file.getOriginalFilename());
		if (!ALLOWED_EXT.contains(ext)) {
			throw new BadRequestException("허용되지 않은 이미지 형식입니다: " + ext);
		}

		String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
		Path target = root.resolve(name).normalize();
		// 방어: 정규화 후에도 반드시 root 하위여야 한다.
		if (!target.startsWith(root)) {
			throw new BadRequestException("잘못된 파일 경로입니다.");
		}
		try {
			Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			throw new IllegalStateException("파일 저장에 실패했습니다.", e);
		}
		return PUBLIC_PREFIX + name;
	}

	/** 공개 경로({@code /uploads/xxx.jpg})의 실제 파일을 지운다. 없으면 조용히 무시. */
	public void deleteByPublicPath(String publicPath) {
		if (publicPath == null || !publicPath.startsWith(PUBLIC_PREFIX)) {
			return;
		}
		String name = publicPath.substring(PUBLIC_PREFIX.length());
		Path target = root.resolve(name).normalize();
		if (!target.startsWith(root)) {
			return;
		}
		try {
			Files.deleteIfExists(target);
		} catch (IOException e) {
			log.warn("업로드 파일 삭제 실패(무시): {}", target, e);
		}
	}

	private String extensionOf(String filename) {
		if (filename == null) {
			return "";
		}
		int dot = filename.lastIndexOf('.');
		return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
	}
}
