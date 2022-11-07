import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState, useRef } from 'react';
import { Buffer } from "buffer";
import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import StationSelect from '../components/station-select';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';

export default function SelectPage(props: any) {

    const paths = props.paths;

    const [station1, setStation1] = useState('');
    const [station2, setStation2] = useState('');

    return (
            paths.isEmpty() ? (
                <ActivityIndicator animating={true} color={MD2Colors.red800} />
            ) : (
                <View style={styles.container}>
                    <StationSelect
                        title={"Select first station"}
                        stationList={paths.stationList()}
                        onSelect={setStation1}
                        selected={station1}
                    />
                    <StationSelect
                        title={"Select second station"}
                        stationList={paths.stationListForStation(station1.id)}
                        onSelect={setStation2}
                        selected={station2}
                    />
                </View>
            )
    )
}

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: 'white',
        padding: 10,
        paddingTop: 100
    },
    titleText: {
        padding: 8,
        fontSize: 16,
        textAlign: 'center',
        fontWeight: 'bold',
    },
    headingText: {
        padding: 8,
    },
});
