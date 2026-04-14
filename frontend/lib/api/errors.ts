export type FieldError = {
  field: string;
  message: string;
};

export type BackendErrorBody = {
  code: string;
  message: string;
  fieldErrors?: FieldError[];
};

export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, body: BackendErrorBody) {
    super(body.message);
    this.name = "ApiError";
    this.status = status;
    this.code = body.code;
    this.fieldErrors = body.fieldErrors ?? [];
  }
}

export class NetworkError extends Error {
  constructor(cause: unknown) {
    super("백엔드 서버에 연결할 수 없습니다.");
    this.name = "NetworkError";
    this.cause = cause;
  }
}
