import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState, useRef, useReducer } from 'react';
import { Buffer } from "buffer";
import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import StationSelect from '../components/station-select';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';
import { actionCreators, initialState, reducer } from '../reducers/store';
import { useCustomContext } from '../reducers/context';
import { Button, TextInput } from 'react-native-paper';
import Schedule from '../models/schedule';
import { NavigationContext } from '@react-navigation/native';

export default function SelectPage() {

    const { state, dispatch } = useCustomContext();
    const [stations, setStations] = useState({ station1: '', station2: '' });
    // TODO: deleteme
    // const [stations, setStations] = useState({ station1: {id: 'ELMHURST'}, station2: {id: 'LOMBARD'} });
    const [loadingSubmit, setLoadingSubmit] = useState(false)
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
        setLoadingSubmit(true)
        dispatch(actionCreators.setActiveSchedule([stations.station1.id, stations.station2.id]))
        navigation.navigate('SchedulePage')
    }

    return (
        state.loadingPaths ? (
            <ActivityIndicator animating={true} color={MD2Colors.red800} />
        ) : (
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
                            loading={loadingSubmit}
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
                        stationList={state.paths.stationList()}
                        onSelect={updateStation1}
                        selected={stations.station1}
                    />
                    <StationSelect
                        title={"Select second station"}
                        stationList={state.paths.stationListForStation(stations.station1.id)}
                        onSelect={updateStation2}
                        selected={stations.station2}
                    />
                </View>
            </React.Fragment>
        )
    )
}

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
