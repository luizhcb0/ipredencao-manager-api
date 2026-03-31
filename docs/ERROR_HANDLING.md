# Error Handling - API

## Overview

The API returns errors in a consistent, structured JSON format through centralized exception handling. Controllers do not contain try-catch blocks — all exceptions are handled by `GlobalExceptionHandler` and `SecurityConfig`.

## Error Response Format

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value for field 'dataNascimento' (expected type: DateTime)",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

| Field | Description |
|---|---|
| `status` | HTTP status code |
| `error` | HTTP status reason phrase |
| `message` | Human-readable description of the error |
| `timestamp` | ISO 8601 timestamp of when the error occurred |

## Error Types by Status Code

### 401 - Unauthorized
**When:** Missing or invalid authentication token. Handled by `SecurityConfig` (`AuthenticationEntryPoint`).

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Authentication token is missing or invalid",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

### 403 - Forbidden
**When:** Authenticated user lacks permission. Handled by `SecurityConfig` (`AccessDeniedHandler`).

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

### 400 - Bad Request
**When:** Malformed JSON, type mismatch, or business rule violation. Handled by `GlobalExceptionHandler`.

Malformed JSON body:
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Invalid value for field 'dataNascimento' (expected type: DateTime)",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

Business rule violation (thrown as `IllegalArgumentException`):
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Já existe um formulário cadastrado com este email: joao@email.com",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

### 404 - Not Found
**When:** Resource not found. Thrown as `NoSuchElementException`.

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Pessoa não encontrada",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

### 409 - Conflict
**When:** State conflict. Thrown as `IllegalStateException`.

### 501 - Not Implemented
**When:** Feature not yet available. Thrown as `UnsupportedOperationException`.

### 500 - Internal Server Error
**When:** Unexpected errors. Catch-all in `GlobalExceptionHandler`.

```json
{
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred",
  "timestamp": "2026-03-18T22:30:00.000Z"
}
```

## Architecture

### Exception → Handler Mapping

| Exception | Handler | Status | Log Level |
|---|---|---|---|
| Missing/invalid token | `SecurityConfig` (AuthenticationEntryPoint) | 401 | — |
| Access denied | `SecurityConfig` (AccessDeniedHandler) | 403 | — |
| `HttpMessageNotReadableException` | `GlobalExceptionHandler` | 400 | WARN |
| `IllegalArgumentException` | `GlobalExceptionHandler` | 400 | WARN |
| `NoSuchElementException` | `GlobalExceptionHandler` | 404 | WARN |
| `IllegalStateException` | `GlobalExceptionHandler` | 409 | WARN |
| `UnsupportedOperationException` | `GlobalExceptionHandler` | 501 | WARN |
| `Exception` (catch-all) | `GlobalExceptionHandler` | 500 | ERROR + stack trace |

### How to Add Error Handling in Controllers

Controllers should **not** use try-catch. Throw the appropriate exception and let the handler do the rest:

```java
@GetMapping("/{id}")
public ResponseEntity<Pessoa> getById(@PathVariable Long id) {
    Pessoa pessoa = pessoaService.findById(id);  // throws NoSuchElementException if not found
    return ResponseEntity.ok(pessoa);
}

@PostMapping
public ResponseEntity<Pessoa> create(@RequestBody Pessoa pessoa) {
    // IllegalArgumentException for validation errors → 400
    // NoSuchElementException for missing references → 404
    Pessoa created = pessoaService.create(pessoa);
    return ResponseEntity.ok(created);
}
```

## Frontend Integration

```javascript
try {
  const response = await fetch('/api/pessoas', { method: 'POST', ... });
  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.message);
  }
  return await response.json();
} catch (error) {
  console.error(error.message);
}
```
