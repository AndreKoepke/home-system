import {environment} from "../environments/environment";

export function getWebsocketBaseUrl(): string {
  if (environment.backend.host) {
    return `${environment.backend.webSocketProtocol}${environment.backend.host}${environment.backend.path}secured/ws/v1/`;
  }

  return `${environment.backend.webSocketProtocol}${window.location.host}${environment.backend.path}secured/ws/v1/`;
}

export function getHttpBaseUrl(): string {
  if (environment.backend.host) {
    return `${environment.backend.protocol}${environment.backend.host}${environment.backend.path}secured/`;
  }

  return `${environment.backend.path}secured/`;
}
