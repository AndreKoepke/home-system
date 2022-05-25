# home-system

Smart homes are rising. But often, a home with programmable switches is called "smart".  
I think, that a home reaches "smartness", when you only have to touch a button rarely.  
This is, what this project is trying to do.

# Feature list
* Build animations with your lights, which will be played when you open your door.
* Turn lights automatically off, when it's bright outside.
* And turn it on again, if it's getting dark.
* Use different states for your home. Like a night- and holiday-state.
* Stay informed via telegram
  * when somebody opens your door
  * when it doesn't rain since a few days
* Use motion sensors to control your lights (even with you, [IKEA motion sensor](https://github.com/dresden-elektronik/deconz-rest-plugin/issues/1676))


# Installation
Currently there is no user-friendly setup. See this [issue](https://github.com/AndreKoepke/home-system/issues/2).  

## Prerequisite
* [deCONZ aka Phoscon](https://phoscon.de/en/conbee/install)
* docker
* (recommended) k8s & helm
* (recommended) hostname with TLS-cert (for telegram webhook)

## Helm with kubernetes (preferred)

```bash
git clone git@github.com:AndreKoepke/home-system.git
cd home-system
mv helm/config/applications.yaml.example helm/config/applications.yaml
nano helm/config/applications.yaml
helm upgrade --install yourHome helm/
```


## Pure docker

You can also start the docker-image directly.
In that case, you have to mount the `application.yaml` directly.

```bash
git clone git@github.com:AndreKoepke/home-system.git
cd home-system
mv helm/config/applications.yaml.example helm/config/applications.yaml
nano helm/config/applications.yaml
docker run -p 8080:8080 -v $(pwd)/helm/config:/app/resources/ akop/home-system
```
For telegram-webhook: Ensure, that your container can be reached from the internet
and behind a reverse-proxy with a TLS-certificate.

---
### Some words about quality
I'm working alone on this project, and I haven't much time for tests.  

<details>
  <summary>Don't judge me for bugs. :)</summary>
  
  ![Meme for testing in production](images/test_in_production.png)
  
</details>

