# home-system

My personal-home system. It using a raspberry with deCONZ behind the scenes.


# SetUp
Currently there is no user-friendly setup. See #issue.  

## Prerequists

* docker
* (optional) k8s
* (optional) helm
* (optional) hostname with TLS-cert (for telegram webhook)

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

