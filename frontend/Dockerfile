FROM node:24-alpine AS build

WORKDIR /usr/src/app
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile

# Bundle app source
COPY . ./
RUN npm run build

# use SSR, so a running node is necessary
FROM node:24-alpine
WORKDIR /usr/app
COPY --from=build /usr/src/app/dist/home-system ./

ADD https://github.com/kyubisation/angular-server-side-configuration/releases/download/v20.0.0/ngssc_64bit /usr/sbin/ngssc
RUN chmod +x /usr/sbin/ngssc

CMD node server/server.mjs
EXPOSE 4000
