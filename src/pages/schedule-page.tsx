import React, { useEffect, useState, useRef } from 'react';
import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import { StyleSheet, Text, View } from 'react-native';
import { Buffer } from "buffer";
import { serverFetch } from '../util/server';
import Schedule from '../models/schedule';
import Paths from '../models/paths';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';

export default function SchedulePage(props: any) {

    const schedule = props.schedule

    return (
        schedule.isEmpty() ? (
            <ActivityIndicator animating={true} color={MD2Colors.red800} />
        ) : (
            <View>
                <Text>{schedule.groups()[0]}</Text>
            </View>
        )
    )
}
