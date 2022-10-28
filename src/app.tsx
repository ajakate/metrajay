import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState, useRef } from 'react';
import { Buffer } from "buffer";
import Paths from './models/paths';
import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'

const initApp = async (setPathsFunc: any) => {
    let response = await fetch(METRAJAY_SERVER_URL + "/paths", {
        headers: {
            Authorization: "Basic " + Buffer.from(BASIC_AUTH_USERNAME + ":" + BASIC_AUTH_PASSWORD).toString('base64')
          },
    });

    let data = await response.json();
    setPathsFunc(new Paths(data));
};


export default function App() {

    const [paths, setPaths] = useState(new Paths({}));

    useEffect(() => {
        initApp(setPaths);
    }, []);

    return (
        <View style={styles.container}>
            <Text>Open up App.tsx to start working on your app!</Text>
            <Text>{paths.getName("103RD-BEV")}</Text>
            <StatusBar style="auto" />
        </View>
    );
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: '#fff',
        alignItems: 'center',
        justifyContent: 'center',
    },
});
