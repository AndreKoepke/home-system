export const environment = {
  backend: {
    protocol: process.env['protocol'],
    host: process.env['host'],
    path: process.env['path'],
    webSocketProtocol: process.env['wsProtocol']
  },
};
