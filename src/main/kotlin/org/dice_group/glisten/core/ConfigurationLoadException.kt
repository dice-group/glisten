package org.dice_group.glisten.core

/**
 * A Configuration load exception provides insight in why a configuration couldn't be loaded (e.g. no file found, wrong format)
 *
 * @param msg the message string to print when this exception is thrown.
 */
class ConfigurationLoadException(msg: String) : Exception(msg)