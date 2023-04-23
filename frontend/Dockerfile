FROM node:18 as builder

WORKDIR /usr/src/app
COPY package.json yarn.lock ./
RUN yarn install --frozen-lockfile && mkdir src

# Bundle app source
COPY *.json ./
COPY src/app ./src/app
COPY src/environments ./src/environments
COPY src/index.html ./src/
COPY src/styles.scss ./src/
COPY src/*.ts ./src/
RUN npm run build


FROM nginxinc/nginx-unprivileged:alpine

COPY --from=builder  /usr/src/app/dist/frontend/ /usr/share/nginx/html/
COPY src/assets /usr/share/nginx/html/assets
COPY nginx/nginx.conf /etc/nginx
COPY nginx/conf.d /etc/nginx/conf.d/
COPY nginx/extra /etc/nginx/conf.d/extra/

EXPOSE 8080
