# WF-Thermos-Eye ESP32 Sniffer
# For use with ESP-IDF v5.1+

cmake_minimum_required(VERSION 3.16)

set(EXTRA_COMPONENT_DIRS $ENV{IDF_PATH}/components/bt/host/nimble)

include($ENV{IDF_PATH}/tools/cmake/project.cmake)
project(wf-sniffer)
