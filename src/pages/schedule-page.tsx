import { NavigationContext } from '@react-navigation/native';
import { observer } from 'mobx-react-lite';
import React, { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { Button, DataTable } from 'react-native-paper';
import { useStore } from '../stores/store';

// TODO: cleanup dependencies in package.json
const SchedulePage = observer(() => {

    const { paths, schedule } = useStore();

    let navigation = React.useContext(NavigationContext);

    let [selectedDate, setSelectedDate] = useState(schedule.groups()[0])
    let [bound, setBound] = useState('inbound')

    const activeTimes = schedule.timesForGroup(selectedDate)[bound]

    // TODO: potentially breaks on chicago kedzie,
    // will break when first row station is ''
    const findStations = () => {
        let inner = schedule.data[0].inner_station;
        let outer = schedule.data[0].outer_station;
        if (bound === 'inbound') {
            return [outer, inner]
        } else {
            return [inner, outer]
        }
    }

    const stations = findStations().map(st => paths.getFullName(st));

    return (
        <View>
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
                                <Button
                                    disabled={selectedDate === g}
                                    mode="contained" style={styles.button}
                                    compact={true}
                                    onPress={() => setSelectedDate(g)}>{g}</Button>
                            </View>
                        )
                    })
                }
            </View>
            <DataTable>
                <DataTable.Header>
                    {
                        stations.map(name => {
                            return <DataTable.Title key={name}>{name}</DataTable.Title>
                        })
                    }
                </DataTable.Header>
                <View style={{ height: 500 }}>
                    <ScrollView >
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
                </View>
            </DataTable>
        </View>
    )
})

export default SchedulePage;

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
