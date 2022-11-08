import React, { useEffect, useState, useRef } from 'react';
import { METRAJAY_SERVER_URL, BASIC_AUTH_USERNAME, BASIC_AUTH_PASSWORD } from '@env'
import { StyleSheet, Text, View, ScrollView } from 'react-native';
import { Buffer } from "buffer";
import { serverFetch } from '../util/server';
import Schedule from '../models/schedule';
import Paths from '../models/paths';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';
import { useCustomContext } from '../reducers/context';
import { NavigationContext } from '@react-navigation/native';
import StationSelect from '../components/station-select';
import { actionCreators } from '../reducers/store';
import { ToggleButton, Chip, Button, DataTable } from 'react-native-paper';

// TODO: cleanup dependencies in package.json
export default function SchedulePage() {

    let { state, dispatch } = useCustomContext();
    let navigation = React.useContext(NavigationContext);
    const schedule = state.schedules[state.activeSchedule]

    let [selectedDate, setSelectedDate] = useState(schedule.groups()[0])
    let [bound, setBound] = useState('inbound')

    const activeTimes = schedule.timesForGroup(selectedDate)[bound]

    const loadingSchedule = () => {
        try {
            if (schedule.keyName()[0]) {
                return false
            } else {
                return true
            }
        } catch {
            return true
        }
    }

    const initPage = async () => {
        if (loadingSchedule()) {
            let stations = state.activeSchedule;
            let schedule = await Schedule.asyncCreate(stations[0], stations[1], state.paths)
            dispatch(actionCreators.addSchedule(schedule))
        }
    }

    useEffect(() => {
        initPage();
    }, []);

    useEffect(() => {
        setSelectedDate(schedule.groups()[0]);
    }, [schedule]);


    return (
        loadingSchedule() ? (
            <ActivityIndicator animating={true} color={MD2Colors.red800} />
        ) : (
            <View>
                <Text>{schedule.keyName()[0]}</Text>
                <View style={styles.buttonContainer}>
                    {
                        ['inbound', 'outbound'].map(g => {
                            return (
                                <View key={g} style={{ flex: 1 }}>
                                    <Button
                                        disabled={bound === g}
                                        mode="contained"
                                        style={styles.button} onPress={() => setBound(g)}>{g}</Button>
                                </View>
                            )
                        })
                    }
                </View>
                <View style={styles.gap} />
                <View style={styles.buttonContainer}>
                    {
                        schedule.groups().map(g => {
                            return (
                                <View key={g} style={{ flex: 1 }}>
                                    <Button disabled={selectedDate === g} mode="contained" style={styles.button} onPress={() => setSelectedDate(g)}>{g}</Button>
                                </View>
                            )
                        })
                    }
                </View>
                <DataTable>
                    <DataTable.Header>
                        <DataTable.Title>Departure</DataTable.Title>
                        <DataTable.Title>Arrival</DataTable.Title>
                    </DataTable.Header>
                    <ScrollView>
                        {
                            activeTimes.map(time => {
                                return (
                                    <DataTable.Row key={time[0]}>
                                        <DataTable.Cell>{time[0]}</DataTable.Cell>
                                        <DataTable.Cell>{time[1]}</DataTable.Cell>
                                    </DataTable.Row>
                                )
                            })
                        }
                    </ScrollView>
                </DataTable>
            </View>
        )
    )
}

const styles = StyleSheet.create({
    buttonContainer: {
        flexDirection: "row",
        alignItems: 'center',
        justifyContent: 'space-around'
    },
    button: {
        margin: 1,
        borderRadius: 0,
        alignSelf: 'stretch'
    },
    gap: {
        marginBottom: 10
    },
});
