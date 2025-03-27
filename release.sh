#!/usr/bin/env bash
# $Id: 4994d8d004ef2d5808f1a2796732b2f3fa44eb09 $

mvn -T 1 -B -P+release clean release:clean release:prepare release:perform
