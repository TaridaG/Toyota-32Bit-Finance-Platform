export const LOGIN_ATTENTION_EVENT = 'finance-login-attention'

export function requestLoginAttention(): void {
  window.dispatchEvent(new CustomEvent(LOGIN_ATTENTION_EVENT))
}
