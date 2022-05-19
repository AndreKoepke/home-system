# home-system

Smart homes are comming. A home reaches "smartness", when you never have to touch a button or switch again.  
This is, what this project is trying to do.

# Feature list
* Build animations with your light, which will be played when you open your door.
* Turn lights automatically off, when it's bright outside.
* And turn it on again, if's getting dark.
* Use different states for your home. Like a night- and holiday-state.
* Stay informed - when somebody opens your door, when it doesn't raining since a few days.
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
        helm upgrade --install yourHome helm/
4. Be happy.

## Pure docker

You can also start the docker-image directly.
In that case, you have to mount the `application.yaml` directly.

```bash
docker run akop/home-system -v config:/config
```
