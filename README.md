# home-system

Smart homes are rising. But often, a home with programmable switches is called "smart".  
I think, that a home reaches "smartness", when you only rarely have to touch a button.  
This is, what this project is trying to do.

# Feature list
* Build animations with your lights, which will be played when you open your door.
* Turn lights automatically off, when it's bright outside.
* And turn it on again, if it's getting dark.
* Use different states for your home. Like a night- and holiday-state.
* Stay informed via telegram
  * when somebody opens your door
  * when it doesn't raining since a few days
* Use motion sensors to control your lights (even with you, [IKEA motion sensor](https://github.com/dresden-elektronik/deconz-rest-plugin/issues/1676))


# SetUp
Currently there is no user-friendly setup. See this [issue](https://github.com/AndreKoepke/home-system/issues/2).  

## Prerequisite
* [deCONZ aka Phoscon](https://phoscon.de/en/conbee/install)
* docker
* (recommended) k8s
* (recommended) helm
* (recommended) hostname with TLS-cert (for telegram webhook)

## Helm with kubernetes (preffered)

1. Checkout this project
2. Change the default applications.yaml 
3. Install the chart with

    <!-- language: bash -->
        git clone project-url
        cd home-system
        nano helm/config/applications.yaml
        helm upgrade --install yourHome helm/
4. Be happy.

## Pure docker

You can also start the docker-image directly.
In that case, you have to mount the `application.yaml` directly.

```bash
docker run akop/home-system -v config:/config
```
