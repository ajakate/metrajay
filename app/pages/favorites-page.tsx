import { NavigationContext } from '@react-navigation/native';
import { observer } from 'mobx-react-lite';
import React, { useEffect, useState } from 'react';
import { ScrollView, StyleSheet, View, Text } from 'react-native';
import { Button, DataTable } from 'react-native-paper';
import { useStore } from '../stores/store';

const FavoritesPage = observer(() => {

    const { favorites, setSchedule, removeFavorite } = useStore();

    const noFavorites = favorites.length == 0;

    let navigation = React.useContext(NavigationContext);

    const gotoSchedule = key => {
        setSchedule(key)
        navigation.navigate('ScheduleScreen')
    }

    const deleteSchedule = key => {
        removeFavorite(key)
    }

    return (
        noFavorites ? (
            <View>
                <Text>You have no favorites. Click below to make one</Text>
                <Button
                    mode="contained"
                    onPress={() => navigation?.navigate('SelectScreen')}
                >
                    Search for a Schedule
                </Button>
            </View>
        ) : (
            <View>
                <DataTable>
                    <DataTable.Header>
                        <DataTable.Title>Route</DataTable.Title>
                    </DataTable.Header>
                    {
                        favorites.map(f => {
                            return (
                                <DataTable.Row key={JSON.stringify(f)}>
                                    <DataTable.Cell>
                                        <View>
                                            <Text>
                                                {
                                                    `${f[0]}${'\n'}${f[1]}`
                                                }
                                            </Text>
                                        </View>
                                    </DataTable.Cell>
                                    <DataTable.Cell>
                                        <Button
                                            mode="contained"
                                            onPress={() => gotoSchedule(f)}
                                        >
                                            View
                                        </Button>
                                    </DataTable.Cell>
                                    <DataTable.Cell>
                                        <Button
                                            mode="elevated"
                                            onPress={() => deleteSchedule(f)}
                                        >
                                            Delete
                                        </Button>
                                    </DataTable.Cell>
                                </DataTable.Row>
                            )
                        })
                    }
                </DataTable>
            </View >
        )
    )
})

export default FavoritesPage;
