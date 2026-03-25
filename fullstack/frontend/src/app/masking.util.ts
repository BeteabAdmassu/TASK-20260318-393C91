export function maskSensitiveInUi(value: string): string {
  if (!value) {
    return value;
  }

  const phoneMasked = value.replace(/(\b\d{3})\d{4}(\d{4}\b)/g, '$1****$2');
  return phoneMasked.replace(/(\b[A-Za-z0-9]{3})[A-Za-z0-9]{8,12}([A-Za-z0-9]{3}\b)/g, '$1********$2');
}
