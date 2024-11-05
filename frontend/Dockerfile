FROM node:20-alpine as build

WORKDIR /usr/src/app
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile

# Bundle app source
COPY . ./
RUN npm run build

FROM node:20-alpine
WORKDIR /usr/app
COPY --from=build /usr/src/app/dist/home-system ./
CMD node server/server.mjs
EXPOSE 4000
