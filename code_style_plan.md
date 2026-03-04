# 리팩토링 계획: 구조 변경으로 변경 난이도 낮추기

## Context

현재 프로젝트는 Service 계층 없이 Controller가 Repository를 직접 사용하고 있다. 코드 스타일도 작성자에 따라 불일치가 많다. 이 상태에서 기능을 추가하거나 수정하면 Controller가 비대해지고 중복 코드가 늘어난다. 이번 작업의 목표는 **작동은 그대로 유지하면서 구조를 정리**하여, 이후 기능 변경이 쉬운 코드베이스를 만드는 것이다.

3단계로 진행한다:
1. **Phase 1**: 스타일 정리 (작동 변경 없음)
2. **Phase 2**: 불필요한 코드 제거 (작동 변경 없음)
3. **Phase 3**: 서비스 계층 추출 (구조 변경, 작동 변경 없음)

---

## Phase 1: 스타일 정리

### 1-1. `@Autowired` 제거 [x]

Spring 4.3+ 단일 생성자 자동 주입. 4개 파일에서 `@Autowired` 어노테이션 + import 제거.

| 파일 | 라인 |
|------|------|
| `gift/member/MemberController.java` | `@Autowired` + import |
| `gift/member/AdminMemberController.java` | `@Autowired` + import |
| `gift/auth/AuthenticationResolver.java` | `@Autowired` + import |
| `gift/auth/JwtProvider.java` | `@Autowired` + import |

### 1-2. 불필요한 클래스 레벨 주석 제거 [x]

Javadoc(`@author`, `@since`)과 블록 주석 제거. 단, `JwtProvider`의 **메서드 레벨** Javadoc은 API 문서 역할이므로 유지.

| 파일 | 제거 대상 |
|------|-----------|
| `gift/member/Member.java` | 클래스 레벨 Javadoc |
| `gift/member/MemberController.java` | 클래스 레벨 Javadoc |
| `gift/member/AdminMemberController.java` | 클래스 레벨 Javadoc |
| `gift/member/MemberRepository.java` | 클래스 레벨 Javadoc |
| `gift/member/MemberRequest.java` | 클래스 레벨 Javadoc |
| `gift/auth/AuthenticationResolver.java` | 클래스 레벨 Javadoc |
| `gift/auth/JwtProvider.java` | 클래스 레벨 Javadoc만 |
| `gift/auth/TokenResponse.java` | 클래스 레벨 Javadoc |
| `gift/auth/KakaoAuthController.java` | 블록 주석 |
| `gift/option/OptionController.java` | 블록 주석 |

### 1-3. `collect(Collectors.toList())` → `.toList()` 통일 [x]

`gift/option/OptionController.java`:
- `.collect(Collectors.toList())` → `.toList()`
- `import java.util.stream.Collectors` 제거

반환값이 응답 직렬화에만 사용되므로 불변 리스트 전환에 문제 없음.

### 1-4. HTTP 상태코드 숫자 → `HttpStatus` 상수 통일 [x]

| 파일 | 변경 |
|------|------|
| `gift/wish/WishController.java` | `401` → `HttpStatus.UNAUTHORIZED`, `403` → `HttpStatus.FORBIDDEN` |
| `gift/order/OrderController.java` | `401` → `HttpStatus.UNAUTHORIZED` |

각 파일에 `import org.springframework.http.HttpStatus` 추가.

### 1-5. `@RequestMapping(path = ...)` → `@RequestMapping("...")` 통일 [x]

| 파일 | 변경 |
|------|------|
| `gift/auth/KakaoAuthController.java` | `path = ` 제거 |
| `gift/option/OptionController.java` | `path = ` 제거 |

### 1-6. 에러 메시지 한국어 통일 [x]

| 파일 | 변경 전 → 변경 후 |
|------|-------------------|
| `gift/member/Member.java` | `"Amount must be greater than zero."` → `"충전 금액은 1 이상이어야 합니다."` |
| `gift/member/MemberController.java` | `"Email is already registered."` → `"이미 등록된 이메일입니다."` |
| `gift/member/MemberController.java` | `"Invalid email or password."` → `"이메일 또는 비밀번호가 올바르지 않습니다."` |
| `gift/member/AdminMemberController.java` | `"Email is already registered."` → `"이미 등록된 이메일입니다."` |
| `gift/member/AdminMemberController.java` | `"Member not found. id="` → `"회원이 존재하지 않습니다. id="` |

---

## Phase 2: 불필요한 코드 제거

### 2-1. `@ExceptionHandler` 중복 → `GlobalExceptionHandler`로 통합 [x]

3개 Controller에 동일한 `@ExceptionHandler(IllegalArgumentException.class)` 코드가 중복.

**신규 파일**: `gift/config/GlobalExceptionHandler.java`

```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**제거 대상**:
| 파일 | 제거 내용 |
|------|-----------|
| `gift/product/ProductController.java` | `@ExceptionHandler` 메서드 + `ExceptionHandler` import |
| `gift/option/OptionController.java` | `@ExceptionHandler` 메서드 + `ExceptionHandler` import |
| `gift/member/MemberController.java` | `@ExceptionHandler` 메서드 + `ExceptionHandler` import |

### 2-2. 보류 항목 (이번 단계에서 건드리지 않음) [x]

- **OrderController의 WishRepository 미사용 주입**: Phase 3에서 OrderService 추출 시 함께 정리
- **Kotlin 플러그인**: 빌드 설정 변경은 위험도 높으므로 별도 작업으로 분리

---

## TDD 테스트 전략

- **Unit 테스트 (Mockito)**: Service 단위 테스트. Repository/외부 의존성 모두 Mock
- **Validator 테스트**: ProductNameValidator, OptionNameValidator 순수 함수 테스트 (Mock 불필요)
- 테스트 언어: Java (기존 프로덕션 코드와 동일)
- 예상 테스트 수: ~75개

### TDD 사이클 (서비스당)

1. **RED**: 컨트롤러에서 동작 목록 추출 → 테스트 메서드 작성 → 빈 서비스 스켈레톤 → 테스트 실패
2. **GREEN**: 컨트롤러 로직을 서비스로 이동 → 테스트 통과
3. **REFACTOR**: 컨트롤러를 서비스 의존으로 변경 → `./gradlew test` + `./gradlew compileJava` 확인

### 테스트 파일 구조

```
src/test/
├── java/gift/
│   ├── TestFixtures.java
│   ├── category/CategoryServiceTest.java
│   ├── product/
│   │   ├── ProductNameValidatorTest.java
│   │   └── ProductServiceTest.java
│   ├── member/MemberServiceTest.java
│   ├── option/
│   │   ├── OptionNameValidatorTest.java
│   │   └── OptionServiceTest.java
│   ├── wish/WishServiceTest.java
│   ├── auth/KakaoAuthServiceTest.java
│   └── order/OrderServiceTest.java
└── resources/
    └── application-test.properties
```

---

## Phase 3: 서비스 계층 추출 [x]

### 공통 설계 원칙

- `@Service` + `@Transactional(readOnly = true)` 클래스 기본
- 변경 메서드에만 `@Transactional` 추가
- 조회 실패 시 `NoSuchElementException`, 비즈니스 규칙 위반 시 `IllegalArgumentException`
- Controller는 HTTP 관심사만 (인증 확인, 응답 생성, 폼 렌더링)

### 사전 작업

#### 0-1. 테스트 설정 파일 생성

**신규**: `src/test/resources/application-test.properties`

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;MODE=MYSQL
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=
spring.flyway.enabled=false
spring.jpa.hibernate.ddl-auto=create-drop
jwt.secret=test-secret-key-that-is-at-least-256-bits-long-for-hmac-sha
jwt.expiration=3600000
kakao.login.client-id=test-client-id
kakao.login.client-secret=test-client-secret
kakao.login.redirect-uri=http://localhost:8080/api/auth/kakao/callback
```

Flyway는 MySQL용 마이그레이션이므로 테스트에서 비활성화, H2 + JPA ddl-auto 사용.

#### 0-2. TestFixtures 유틸 생성

**신규**: `src/test/java/gift/TestFixtures.java`

Entity 생성 팩토리 메서드 모음 (Category, Product, Member, Option 등). 모든 테스트에서 공유.

#### 0-3. GlobalExceptionHandler 확장

Phase 3 시작 전에 핸들러 추가:

```java
@ExceptionHandler(NoSuchElementException.class)
public ResponseEntity<String> handleNoSuchElement(NoSuchElementException e) {
    return ResponseEntity.notFound().build();
}

@ExceptionHandler(IllegalStateException.class)
public ResponseEntity<String> handleIllegalState(IllegalStateException e) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
}
```

기존 `orElse(null)` + null 체크 → 404 패턴이 `orElseThrow()` → `NoSuchElementException` → 404로 대체되어 외부 응답 동일.
기존 `status(403)` → `IllegalStateException` → 403으로 대체되어 외부 응답 동일.

### 3-1. CategoryService

**신규**: `gift/category/CategoryService.java`
**수정**: `gift/category/CategoryController.java` (Repository → Service 의존)

```java
@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    // findAll(): List<Category>
    // create(CategoryRequest): Category
    // update(Long id, CategoryRequest): Category  ← orElseThrow
    // delete(Long id): void
}
```

**테스트** (`CategoryServiceTest`, ~5개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findAll_returnsAllCategories` | Repository 위임 + 응답 매핑 |
| `create_savesAndReturns` | save 호출 + 반환 |
| `update_existingCategory_updatesFields` | findById → update → save |
| `update_nonExistent_throwsNoSuchElementException` | orElseThrow 동작 |
| `delete_delegatesToRepository` | deleteById 호출 |

### 3-2. ProductService

**신규**: `gift/product/ProductService.java`
**수정**: `gift/product/ProductController.java`, `gift/product/AdminProductController.java`

```java
@Service
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    // findAll(Pageable): Page<Product>
    // findAll(): List<Product>           ← Admin 목록용
    // findById(Long id): Product         ← orElseThrow
    // create(ProductRequest): Product    ← validateName(allowKakao=false) + 카테고리 조회
    // update(Long id, ProductRequest): Product
    // delete(Long id): void
    // findCategoryById(Long id): Category ← Admin 폼용
    // findAllCategories(): List<Category> ← Admin 폼용
}
```

**AdminProductController 특이사항**: Admin은 `ProductNameValidator.validate(name, true)`로 "카카오" 허용 + 에러를 폼에 표시해야 한다. Admin Controller에서 직접 `ProductNameValidator.validate()`를 호출하여 에러 리스트를 폼에 전달하고, 검증 통과 시 Service의 저장 메서드를 호출하는 구조로 한다. 즉 Admin 검증은 Controller에 남기고, Service에는 별도 `saveProduct(name, price, imageUrl, category)` / `updateProduct(id, name, price, imageUrl, category)` 메서드를 둔다.

**테스트 — ProductNameValidatorTest** (~6개):

| 테스트 | 검증 내용 |
|--------|----------|
| `validate_validName_returnsEmpty` | 정상 이름 |
| `validate_exceedsMaxLength_returnsError` | 15자 초과 |
| `validate_invalidChars_returnsError` | 허용 외 특수문자 |
| `validate_containsKakao_returnsError` | "카카오" 포함 |
| `validate_containsKakao_allowTrue_noError` | allowKakao=true |
| `validate_blankName_returnsError` | 빈 문자열 |

**테스트 — ProductServiceTest** (~11개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findAll_returnsPage` | Pageable 위임 |
| `findById_existing_returns` | 정상 조회 |
| `findById_nonExistent_throws` | NoSuchElementException |
| `create_validRequest_saves` | 검증 + 카테고리 조회 + 저장 |
| `create_invalidName_throws` | 이름 검증 실패 |
| `create_categoryNotFound_throws` | 카테고리 없음 |
| `create_kakaoName_allowFalse_throws` | API 경로 "카카오" 차단 |
| `create_kakaoName_allowTrue_succeeds` | Admin 경로 "카카오" 허용 |
| `update_happyPath_updates` | 전체 필드 업데이트 |
| `update_notFound_throws` | 존재하지 않는 상품 |
| `delete_delegatesToRepository` | 삭제 위임 |

### 3-3. MemberService

**신규**: `gift/member/MemberService.java`
**수정**: `gift/member/MemberController.java`, `gift/member/AdminMemberController.java`

```java
@Service
@Transactional(readOnly = true)
public class MemberService {
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    // findAll(): List<Member>
    // findById(Long id): Member           ← orElseThrow
    // register(String email, String pw): TokenResponse  ← 중복 검증 + 저장 + JWT
    // login(String email, String pw): TokenResponse     ← 조회 + 비밀번호 검증 + JWT
    // create(String email, String pw): Member           ← Admin용, 중복 검증 + 저장
    // update(Long id, String email, String pw): Member
    // chargePoint(Long id, int amount): void
    // delete(Long id): void
}
```

**테스트** (`MemberServiceTest`, ~11개):

| 테스트 | 검증 내용 |
|--------|----------|
| `register_newEmail_savesAndReturnsToken` | 중복 검증 + 저장 + JWT |
| `register_duplicateEmail_throws` | existsByEmail → IllegalArgumentException |
| `login_validCredentials_returnsToken` | findByEmail + 비밀번호 + JWT |
| `login_emailNotFound_throws` | 이메일 없음 |
| `login_wrongPassword_throws` | 비밀번호 불일치 |
| `findAll_returnsList` | Admin 목록 |
| `findById_existing_returns` | Admin 조회 |
| `findById_notFound_throws` | NoSuchElementException |
| `update_updatesFields` | email, password 변경 |
| `chargePoint_validAmount_updates` | chargePoint 호출 + 저장 |
| `delete_delegatesToRepository` | 삭제 위임 |

### 3-4. OptionService

**신규**: `gift/option/OptionService.java`
**수정**: `gift/option/OptionController.java`

```java
@Service
@Transactional(readOnly = true)
public class OptionService {
    private final OptionRepository optionRepository;
    private final ProductRepository productRepository;

    // findByProductId(Long productId): List<Option>  ← 상품 존재 확인 포함
    // create(Long productId, OptionRequest): Option  ← 이름 검증 + 상품 확인 + 중복 검증
    // delete(Long productId, Long optionId): void    ← 최소 1개 유지 + 소속 확인
}
```

**테스트 — OptionNameValidatorTest** (~5개):

| 테스트 | 검증 내용 |
|--------|----------|
| `validate_validName_returnsEmpty` | 정상 이름 |
| `validate_exceedsMaxLength_returnsError` | 50자 초과 |
| `validate_invalidChars_returnsError` | 허용 외 특수문자 |
| `validate_blankName_returnsError` | 빈 문자열 |
| `validate_validSpecialChars_returnsEmpty` | 허용 특수문자 `()[]+-&/_` |

**테스트 — OptionServiceTest** (~10개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findByProductId_exists_returnsOptions` | 상품 확인 + 옵션 목록 |
| `findByProductId_productNotFound_throws` | NoSuchElementException |
| `create_happyPath_saves` | 이름 검증 + 상품 확인 + 중복 확인 + 저장 |
| `create_invalidName_throws` | 이름 검증 실패 |
| `create_productNotFound_throws` | 상품 없음 |
| `create_duplicateName_throws` | 동일 상품 내 중복 이름 |
| `delete_happyPath_deletes` | 정상 삭제 |
| `delete_onlyOneOption_throws` | 최소 1개 옵션 제약 |
| `delete_optionNotOwnedByProduct_throws` | 소속 확인 |
| `delete_productNotFound_throws` | 상품 없음 |

### 3-5. WishService

**신규**: `gift/wish/WishService.java`
**수정**: `gift/wish/WishController.java`

```java
@Service
@Transactional(readOnly = true)
public class WishService {
    private final WishRepository wishRepository;
    private final ProductRepository productRepository;

    // findByMemberId(Long memberId, Pageable): Page<Wish>
    // add(Long memberId, Long productId): AddResult  ← 상품 확인 + 중복 처리
    // remove(Long memberId, Long wishId): void       ← 존재 확인 + 소유권 검증
}
```

**addWish 201/200 분기 보존**: 기존 코드는 중복이면 200, 신규면 201을 반환한다. 이를 유지하기 위해 Service에서 `record AddResult(Wish wish, boolean created)`를 반환하고 Controller에서 분기한다.

```java
// WishService
public record AddResult(Wish wish, boolean created) {}

@Transactional
public AddResult add(Long memberId, Long productId) {
    Product product = productRepository.findById(productId)
        .orElseThrow(() -> new NoSuchElementException("상품이 존재하지 않습니다."));
    return wishRepository.findByMemberIdAndProductId(memberId, productId)
        .map(existing -> new AddResult(existing, false))
        .orElseGet(() -> new AddResult(wishRepository.save(new Wish(memberId, product)), true));
}

// WishController
var result = wishService.add(member.getId(), request.productId());
if (result.created()) {
    return ResponseEntity.created(URI.create("/api/wishes/" + result.wish().getId()))
        .body(WishResponse.from(result.wish()));
}
return ResponseEntity.ok(WishResponse.from(result.wish()));
```

`removeWish`의 기존 403 응답: 소유권 불일치 시 `IllegalStateException` 발생 → GlobalExceptionHandler가 403으로 변환.

**테스트** (`WishServiceTest`, ~7개):

| 테스트 | 검증 내용 |
|--------|----------|
| `findByMemberId_returnsPagedWishes` | Pageable 위임 |
| `add_newWish_returnsCreatedTrue` | AddResult(wish, created=true) |
| `add_duplicateWish_returnsCreatedFalse` | AddResult(existing, created=false) |
| `add_productNotFound_throws` | NoSuchElementException |
| `remove_happyPath_deletes` | 정상 삭제 |
| `remove_wishNotFound_throws` | NoSuchElementException |
| `remove_notOwner_throwsIllegalState` | 소유권 → IllegalStateException → 403 |

### 3-6. KakaoAuthService

**신규**: `gift/auth/KakaoAuthService.java`
**수정**: `gift/auth/KakaoAuthController.java`

```java
@Service
public class KakaoAuthService {
    private final KakaoLoginClient kakaoLoginClient;
    private final MemberRepository memberRepository;
    private final JwtProvider jwtProvider;

    // loginOrRegister(String code): TokenResponse
    //   ← 토큰 교환 + 사용자 정보 조회 + 회원 조회/생성 + JWT 발급
}
```

KakaoAuthController는 `KakaoAuthService` + `KakaoLoginProperties` 의존. `/login` 엔드포인트의 리다이렉트 URL 구성은 HTTP 관심사이므로 Controller에 남김.

**테스트** (`KakaoAuthServiceTest`, ~5개):

| 테스트 | 검증 내용 |
|--------|----------|
| `loginOrRegister_existingMember_updatesTokenReturnsJwt` | 기존 회원 → 토큰 갱신 |
| `loginOrRegister_newMember_createsAndReturnsJwt` | 신규 회원 생성 |
| `loginOrRegister_savesKakaoAccessToken` | kakaoAccessToken 저장 검증 |
| `loginOrRegister_generatesJwtWithEmail` | JWT 이메일 일치 |
| `loginOrRegister_kakaoClientError_propagates` | 예외 전파 (삼키지 않음) |

Mock 대상: `KakaoLoginClient`, `MemberRepository`, `JwtProvider`

### 3-7. OrderService

**신규**: `gift/order/OrderService.java`
**수정**: `gift/order/OrderController.java`

```java
@Service
@Transactional(readOnly = true)
public class OrderService {
    private final OrderRepository orderRepository;
    private final OptionRepository optionRepository;
    private final MemberRepository memberRepository;
    private final KakaoMessageClient kakaoMessageClient;

    // findByMemberId(Long memberId, Pageable): Page<Order>
    // placeOrder(Member member, OrderRequest): Order
    //   ← 옵션 확인 + 재고 차감 + 포인트 차감 + 저장 + 카카오 알림
}
```

- `WishRepository` 주입 제거 (현재 미사용. wish cleanup은 미구현 상태 유지)
- `sendKakaoMessageIfPossible()` private 메서드를 OrderService로 이동
- Controller에는 `AuthenticationResolver` + `OrderService` 의존만 남김

**테스트** (`OrderServiceTest`, ~10개):

| 테스트 | 검증 내용 |
|--------|----------|
| `placeOrder_happyPath_completesAllSteps` | 전체 흐름 |
| `placeOrder_optionNotFound_throws` | 옵션 없음 |
| `placeOrder_insufficientStock_throws` | 재고 부족 |
| `placeOrder_insufficientPoints_throws` | 포인트 부족 |
| `placeOrder_savesOrderWithCorrectFields` | 주문 엔티티 필드 검증 |
| `placeOrder_calculatesCorrectPrice` | price × quantity |
| `placeOrder_withKakaoToken_sendsNotification` | 알림 전송 |
| `placeOrder_withoutKakaoToken_skipsNotification` | 알림 건너뜀 |
| `placeOrder_notificationFails_orderStillSucceeds` | 알림 실패해도 주문 성공 |
| `findByMemberId_returnsPagedOrders` | 페이징 위임 |

Mock 대상: `OrderRepository`, `OptionRepository`, `MemberRepository`, `KakaoMessageClient`

### Phase 3 실행 순서

```
1. GlobalExceptionHandler 확장 (NoSuchElementException, IllegalStateException)
2. CategoryService → CategoryController 수정
3. ProductService → ProductController, AdminProductController 수정
4. MemberService → MemberController, AdminMemberController 수정
5. OptionService → OptionController 수정
6. WishService → WishController 수정
7. KakaoAuthService → KakaoAuthController 수정
8. OrderService → OrderController 수정
```

---

## 검증 방법

각 서비스 추출 후:

1. `./gradlew test` — 해당 서비스 테스트 전체 통과 + 회귀 없음
2. `./gradlew compileJava` — 컴파일 통과

최종 완료 후:

3. `./gradlew test` — 전체 ~75개 테스트 통과
4. `./gradlew bootRun` — 애플리케이션 정상 기동 (H2 인메모리)
5. 주요 엔드포인트 수동 확인:
   - `GET /api/categories` → 200 + JSON 배열
   - `POST /api/products` 잘못된 이름 → 400 + 에러 메시지
   - `GET /api/products?page=0` → 200 + 페이지 응답
   - `GET /admin/products` → 200 + HTML
   - 존재하지 않는 리소스 → 404

---

# Step2 리팩토링 계획

## Context

Step1에서 서비스 계층 추출과 TDD 기반 테스트(~75개)를 완성한 상태이다.
Step2에서는 트랜잭션 경계 정리, 누락된 도메인 검증 추가, 책임 위치 교정을 수행한다.
**구조 변경(리팩토링)과 작동 변경(새 기능)은 커밋을 반드시 분리한다.**

---

## Phase 1: 트랜잭션 내 불필요한 save() 제거 (구조 변경 3커밋)

JPA dirty checking이 `@Transactional` 내 관리 엔티티의 변경을 자동 반영하므로 명시적 `save()` 호출이 불필요하다.

### 커밋 1-A: `refactor(member): 트랜잭션 내 불필요한 save() 호출 제거`
- `MemberService.java:61` — `update()`에서 `return memberRepository.save(member)` → `return member`
- `MemberService.java:68` — `chargePoint()`에서 `memberRepository.save(member)` 제거
- `MemberServiceTest.java:117` — `given(memberRepository.save(...))` 목 설정 제거
- `MemberServiceTest.java:131` — `then(memberRepository).should().save(member)` 검증 제거

### 커밋 1-B: `refactor(order): 트랜잭션 내 관리 엔티티의 불필요한 save() 호출 제거`
- `OrderService.java:41` — `optionRepository.save(option)` 제거
- `OrderService.java:45` — `memberRepository.save(member)` 제거
- `OrderServiceTest.java` — `save()` 관련 목 설정/검증 제거 (happy path 포함 5개 테스트)

### 커밋 1-C: `refactor(product, category): 트랜잭션 내 관리 엔티티의 불필요한 save() 호출 제거`
- `ProductService.java:48` — `update()`에서 `return productRepository.save(product)` → `return product`
- `CategoryService.java:30` — `update()`에서 `return categoryRepository.save(category)` → `return category`
- 각 테스트의 해당 `save()` 목 설정 제거

---

## Phase 2: KakaoAuthService 트랜잭션 경계 추가 (구조 변경 1커밋)

### 커밋 2: `refactor(auth): KakaoAuthService.loginOrRegister()에 @Transactional 추가`
- `KakaoAuthService.java:32` — `loginOrRegister()` 메서드에 `@Transactional` 추가
- 회원 조회/생성/토큰 업데이트/저장이 하나의 트랜잭션으로 묶여 원자성 보장
- 테스트 변경 없음 (목 기반이라 트랜잭션 동작에 영향 없음)

---

## Phase 3: 인증 null 체크 패턴 제거 — 도메인 책임 개선 1 (구조 변경 1커밋)

### 커밋 3: `refactor(auth): 인증 실패 시 예외를 던지도록 변경하여 컨트롤러 중복 제거`

현재 `OrderController`와 `WishController`에서 동일한 인증 null 체크가 5회 반복된다.

**변경 전:**
```java
var member = authenticationResolver.extractMember(authorization);
if (member == null) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
}
```

**변경 후:** `AuthenticationResolver`가 인증 실패 시 예외를 던지고, `GlobalExceptionHandler`가 401 반환

- 신규 파일: `gift/auth/UnauthorizedException.java` — `RuntimeException` 상속
- `AuthenticationResolver.java` — `extractMember()`에서 null 대신 `UnauthorizedException` 던지기
- `GlobalExceptionHandler.java` — `UnauthorizedException` → 401 핸들러 추가
- `OrderController.java` — null 체크 2개 제거, `ResponseEntity<?>` → 구체 타입으로 변경
- `WishController.java` — null 체크 3개 제거

---

## Phase 4: 관리자 컨트롤러 중복 검증 제거 — 도메인 책임 개선 2 (구조 변경 1커밋)

### 커밋 4: `refactor(product): 관리자 컨트롤러의 중복 이름 검증을 서비스로 위임`

현재 `AdminProductController`가 `ProductNameValidator.validate(name, true)`을 직접 호출하는데, 이는 서비스의 책임이다. 또한 컨트롤러는 `allowKakao=true`로 호출하지만 서비스는 `allowKakao=false`로 호출하여 어차피 서비스에서 거부된다.

- `ProductService.java` — `create(request, allowKakao)`, `update(id, request, allowKakao)` 오버로드 추가
- `AdminProductController.java` — `ProductNameValidator` 직접 호출 제거, 서비스에 `allowKakao=true` 위임. 서비스에서 던지는 `IllegalArgumentException`을 catch하여 폼 에러 처리
- `populateNewForm()`/`populateEditForm()` 시그니처에서 errors 파라미터를 `List<String>` → `String` 또는 유지

---

## Phase 5: 카카오 로그인 회원 생성 로직 이동 — 도메인 책임 개선 3 (구조 변경 1커밋)

### 커밋 5: `refactor(member): 카카오 로그인 시 회원 조회/생성 로직을 MemberService로 이동`

`KakaoAuthService`가 직접 `MemberRepository`를 사용해 회원을 생성하는 것은 `MemberService`의 책임 누수이다.

- `MemberService.java` — `findOrCreateByEmail(String email)` 메서드 추가
- `KakaoAuthService.java` — `MemberRepository` 의존성을 `MemberService`로 교체
- `KakaoAuthServiceTest.java` — `@Mock MemberRepository` → `@Mock MemberService`로 변경, 목 설정 수정

---

## Phase 6: 도메인 자체 검증 추가 (작동 변경 2커밋, 테스트 증거 포함)

### 커밋 6-A: `feat(option): Option 생성 시 수량 양수 검증 추가`
- `Option.java` 생성자 — `if (quantity < 1) throw new IllegalArgumentException("옵션 수량은 1 이상이어야 합니다.")`
- 신규 테스트 `OptionTest.java`:
  - `constructor_zeroQuantity_throwsException` — 수량 0으로 생성 시 예외
  - `constructor_negativeQuantity_throwsException` — 음수 수량으로 생성 시 예외
  - `constructor_validQuantity_createsSuccessfully` — 정상 수량 생성 후 `getQuantity()` 상태 검증
  - `subtractQuantity_toZero_succeeds` — 전량 차감 후 `getQuantity() == 0` 상태 검증

### 커밋 6-B: `feat(product): Product 생성 시 가격 양수 검증 추가`
- `Product.java` 생성자 — `if (price < 1) throw new IllegalArgumentException("상품 가격은 1 이상이어야 합니다.")`
- `Product.java` `update()` — 동일 검증 추가
- 신규 테스트 `ProductTest.java`:
  - `constructor_zeroPrice_throwsException` — 가격 0으로 생성 시 예외
  - `constructor_negativePrice_throwsException` — 음수 가격으로 생성 시 예외
  - `constructor_validPrice_createsSuccessfully` — 정상 가격 생성 후 `getPrice()` 상태 검증
  - `update_zeroPrice_throwsException` — 가격 0으로 수정 시 예외

---

## Phase 7: 옵션 최소 개수 규칙을 Product 도메인으로 이동 (구조 1커밋 + 작동 1커밋)

### 커밋 7-A: `refactor(product): 옵션 삭제 시 최소 1개 규칙을 Product 도메인으로 이동` (구조 변경)
- `Product.java` — `removeOption(Option option)` 메서드 추가: `options.size() <= 1`이면 예외, 아니면 `options.remove(option)` (orphanRemoval로 DB 삭제)
- `OptionService.java` — `delete()`에서 직접 size 체크와 `optionRepository.delete()` 대신 `product.removeOption(option)` 호출

### 커밋 7-B: `test(product): Product.removeOption() 도메인 규칙 테스트 추가` (작동 증거)
- `ProductTest.java`에 추가:
  - `removeOption_lastOption_throwsException` — 옵션 1개인 Product에서 삭제 시 예외
  - `removeOption_multipleOptions_removesSuccessfully` — 옵션 2개 중 1개 삭제 후 `getOptions().size() == 1` 상태 검증

---

## 수정 대상 파일 요약

| 파일 | Phase |
|------|-------|
| `src/main/java/gift/member/MemberService.java` | 1-A, 5 |
| `src/main/java/gift/order/OrderService.java` | 1-B |
| `src/main/java/gift/product/ProductService.java` | 1-C, 4 |
| `src/main/java/gift/category/CategoryService.java` | 1-C |
| `src/main/java/gift/auth/KakaoAuthService.java` | 2, 5 |
| `src/main/java/gift/auth/AuthenticationResolver.java` | 3 |
| `src/main/java/gift/auth/UnauthorizedException.java` | 3 (신규) |
| `src/main/java/gift/config/GlobalExceptionHandler.java` | 3 |
| `src/main/java/gift/order/OrderController.java` | 3 |
| `src/main/java/gift/wish/WishController.java` | 3 |
| `src/main/java/gift/product/AdminProductController.java` | 4 |
| `src/main/java/gift/option/Option.java` | 6-A |
| `src/main/java/gift/product/Product.java` | 6-B, 7-A |
| `src/main/java/gift/option/OptionService.java` | 7-A |
| `src/test/java/gift/member/MemberServiceTest.java` | 1-A |
| `src/test/java/gift/order/OrderServiceTest.java` | 1-B |
| `src/test/java/gift/product/ProductServiceTest.java` | 1-C |
| `src/test/java/gift/category/CategoryServiceTest.java` | 1-C |
| `src/test/java/gift/auth/KakaoAuthServiceTest.java` | 5 |
| `src/test/java/gift/option/OptionTest.java` | 6-A (신규) |
| `src/test/java/gift/product/ProductTest.java` | 6-B, 7-B (신규) |

## 검증 방법

각 커밋마다 `./gradlew test`로 전체 테스트 통과 확인.
작동 변경 커밋(6-A, 6-B, 7-B)은 상태 재조회 방식의 단위 테스트로 검증.

## 요구사항 충족 매트릭스

| 요구사항 | 충족 커밋 |
|---------|----------|
| 트랜잭션 경계 세우기 | 1-A, 1-B, 1-C, 2 |
| 누락된 작동 구현 + 테스트 증거 | 6-A, 6-B |
| 도메인 책임 되찾기 (2개 이상) | 3 (인증 중복 제거), 4 (검증 위임), 5 (회원 생성), 7-A (옵션 규칙) |
| 구조/작동 커밋 분리 | 모든 Phase에서 분리 |

---

# Step2 추가 개선: 예외 처리 체계화 + 컨트롤러 테스트

## Context

Step2 리팩토링(Phase 1~7)이 완료되어 85개 테스트가 통과하는 상태이다.
현재 두 가지 문제가 있다:

1. **예외 처리 비체계적** — 모든 비즈니스 에러가 `IllegalArgumentException` 하나로 처리되어 에러 구분이 불가능하고, 응답 형식이 일관되지 않음 (400은 String body, 404는 body 없음)
2. **컨트롤러 테스트 0개** — HTTP 매핑, 상태 코드, 인증 흐름, 입력 검증이 전혀 검증되지 않음

**Phase 8에서 에러 응답 구조를 먼저 정리한 뒤, Phase 9에서 컨트롤러 테스트로 검증한다.**

---

## Phase 8: 예외 처리 체계화 (구조 변경 2커밋)

### 커밋 8-A: `refactor(config): 통일된 에러 응답 구조 도입`

현재 에러 응답이 일관되지 않다:
- 400: `ResponseEntity<String>` (메시지만)
- 401: `ResponseEntity<Void>` (body 없음)
- 403: `ResponseEntity<String>` (메시지만)
- 404: `ResponseEntity<String>` (body 없음)

**변경:**

- 신규 `gift/config/ErrorResponse.java` — `record ErrorResponse(String code, String message)`
- `GlobalExceptionHandler.java` 수정:
  - `IllegalArgumentException` → `400 + ErrorResponse("BAD_REQUEST", e.getMessage())`
  - `NoSuchElementException` → `404 + ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다.")`
  - `IllegalStateException` → `403 + ErrorResponse("FORBIDDEN", e.getMessage())`
  - `UnauthorizedException` → `401 + ErrorResponse("UNAUTHORIZED", e.getMessage())`
- 기존 테스트 변경 없음 (서비스 테스트는 예외 타입만 검증, HTTP 응답 형식은 검증하지 않음)

### 커밋 8-B: `refactor(config): MethodArgumentNotValidException 핸들러 추가`

현재 `@Valid` 검증 실패 시 Spring 기본 에러 응답이 반환된다. 이를 `ErrorResponse` 형식으로 통일한다.

- `GlobalExceptionHandler.java` — `MethodArgumentNotValidException` 핸들러 추가
  - 첫 번째 필드 에러 메시지를 `ErrorResponse("VALIDATION_FAILED", message)`로 반환
  - 400 상태코드

---

## Phase 9: 컨트롤러 테스트 추가 (테스트 5커밋)

`@WebMvcTest` + `MockMvc`로 HTTP 계층을 검증한다. 서비스는 `@MockBean`으로 목킹.
`AuthenticationResolver`도 `@MockBean`으로 주입하여 인증 필요 컨트롤러 테스트.

### 커밋 9-A: `test(product): ProductController 테스트 추가`

`src/test/java/gift/product/ProductControllerTest.java` (신규, ~7개 테스트)

| 테스트 | 검증 |
|--------|------|
| `getProducts_returnsPagedProducts` | GET /api/products → 200 + JSON |
| `getProduct_existing_returns200` | GET /api/products/1 → 200 + JSON |
| `getProduct_notFound_returns404` | GET /api/products/99 → 404 + ErrorResponse |
| `createProduct_valid_returns201` | POST /api/products + valid body → 201 |
| `createProduct_invalidName_returns400` | POST /api/products + blank name → 400 |
| `updateProduct_valid_returns200` | PUT /api/products/1 + valid body → 200 |
| `deleteProduct_returns204` | DELETE /api/products/1 → 204 |

### 커밋 9-B: `test(category): CategoryController 테스트 추가`

`src/test/java/gift/category/CategoryControllerTest.java` (신규, ~5개 테스트)

| 테스트 | 검증 |
|--------|------|
| `getCategories_returnsList` | GET /api/categories → 200 + JSON 배열 |
| `createCategory_valid_returns201` | POST /api/categories + valid body → 201 |
| `updateCategory_valid_returns200` | PUT /api/categories/1 + valid body → 200 |
| `updateCategory_notFound_returns404` | PUT /api/categories/99 → 404 + ErrorResponse |
| `deleteCategory_returns204` | DELETE /api/categories/1 → 204 |

### 커밋 9-C: `test(order): OrderController 인증 포함 테스트 추가`

`src/test/java/gift/order/OrderControllerTest.java` (신규, ~5개 테스트)

| 테스트 | 검증 |
|--------|------|
| `getOrders_authenticated_returns200` | GET + 유효 토큰 → 200 |
| `getOrders_unauthenticated_returns401` | GET + 잘못된 토큰 → 401 + ErrorResponse |
| `createOrder_authenticated_returns201` | POST + 유효 토큰 + valid body → 201 |
| `createOrder_unauthenticated_returns401` | POST + 토큰 없음 → 401 |
| `createOrder_invalidBody_returns400` | POST + 유효 토큰 + invalid body → 400 |

### 커밋 9-D: `test(wish): WishController 인증 포함 테스트 추가`

`src/test/java/gift/wish/WishControllerTest.java` (신규, ~5개 테스트)

| 테스트 | 검증 |
|--------|------|
| `getWishes_authenticated_returns200` | GET + 유효 토큰 → 200 |
| `getWishes_unauthenticated_returns401` | GET + 잘못된 토큰 → 401 |
| `addWish_new_returns201` | POST + created=true → 201 |
| `addWish_duplicate_returns200` | POST + created=false → 200 |
| `removeWish_returns204` | DELETE + 유효 토큰 → 204 |

### 커밋 9-E: `test(member): MemberController 테스트 추가`

`src/test/java/gift/member/MemberControllerTest.java` (신규, ~4개 테스트)

| 테스트 | 검증 |
|--------|------|
| `register_valid_returns201` | POST /api/members/register → 201 + TokenResponse |
| `register_duplicateEmail_returns400` | POST + 중복 이메일 → 400 + ErrorResponse |
| `login_valid_returns200` | POST /api/members/login → 200 + TokenResponse |
| `login_invalidCredentials_returns400` | POST + 잘못된 비밀번호 → 400 + ErrorResponse |

---

## 수정/생성 대상 파일 요약

| 파일 | Phase |
|------|-------|
| `src/main/java/gift/config/ErrorResponse.java` | 8-A (신규) |
| `src/main/java/gift/config/GlobalExceptionHandler.java` | 8-A, 8-B |
| `src/test/java/gift/product/ProductControllerTest.java` | 9-A (신규) |
| `src/test/java/gift/category/CategoryControllerTest.java` | 9-B (신규) |
| `src/test/java/gift/order/OrderControllerTest.java` | 9-C (신규) |
| `src/test/java/gift/wish/WishControllerTest.java` | 9-D (신규) |
| `src/test/java/gift/member/MemberControllerTest.java` | 9-E (신규) |

## 검증 방법

- 각 커밋마다 `./gradlew test`로 전체 테스트 통과 확인
- Phase 8 완료 후 기존 85개 테스트 회귀 없음 확인
- Phase 9 완료 후 예상 총 테스트 수: ~111개 (85 + 26)
