FROM node:24-alpine
WORKDIR /usr/app
ADD dist/home-system  ./
CMD node server/server.mjs
EXPOSE 4000
