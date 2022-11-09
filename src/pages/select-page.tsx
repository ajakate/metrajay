import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState, useRef, useReducer } from 'react';
import { Buffer } from "buffer";
import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import StationSelect from '../components/station-select';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';
import { Button, TextInput } from 'react-native-paper';
import Schedule from '../models/schedule';
import { NavigationContext } from '@react-navigation/native';
import { observer } from 'mobx-react-lite';
import { useStore } from '../stores/store';

// TODO: remove mox-react from package json
const SelectPage = observer(() => {

    const { paths, setSchedule } = useStore();
    const empty = {id: '', name: ''}
    const [stations, setStations] = useState({ station1: empty, station2: empty });
    // TODO: deleteme
    // const [stations, setStations] = useState({ station1: {id: 'ELMHURST'}, station2: {id: 'LOMBARD'} });
    const navigation = React.useContext(NavigationContext);

    const clearSelections = () => {
        setStations({ station1: '', station2: '' })
    }

    const updateStation1 = (val) => {
        setStations({ ...stations, station1: val })
    }

    const updateStation2 = (val) => {
        setStations({ ...stations, station2: val })
    }

    const canSubmit = (stations.station1 !== "" && stations.station2 !== "")

    const doSubmit = async () => {
        setSchedule([stations.station1.id, stations.station2.id])
        navigation.navigate('ScheduleScreen')
    }

    return (
        <React.Fragment>
            <View style={styles.buttonContainer}>
                <View style={styles.button}>
                    <Button
                        mode="elevated"
                        onPress={() => clearSelections()}
                        icon="eraser">
                        clear
                    </Button>
                </View>
                <View style={styles.button}>
                    <Button
                        mode="contained"
                        disabled={!canSubmit}
                        onPress={doSubmit}
                        icon="arrow-right-bold">
                        submit
                    </Button>
                </View>
            </View>
            <View style={styles.container}>
                <StationSelect
                    title={"Select first station"}
                    stationList={paths.stationList()}
                    onSelect={updateStation1}
                    selected={stations.station1}
                />
                <StationSelect
                    title={"Select second station"}
                    stationList={paths.stationListForStation(stations.station1.id)}
                    onSelect={updateStation2}
                    selected={stations.station2}
                />
            </View>
        </React.Fragment>
    )
})

export default SelectPage;

const styles = StyleSheet.create({
    container: {
        flex: 1,
        backgroundColor: 'white',
        padding: 10,
        paddingTop: 20
    },
    buttonContainer: {
        flexDirection: "row",
        backgroundColor: 'white',
    },
    button: {
        marginLeft: 10
    },
});
