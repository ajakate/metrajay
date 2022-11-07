import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState, useRef } from 'react';
import { Buffer } from "buffer";
import Paths from './models/paths';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';
import SelectPage from './pages/select-page';
import SchedulePage from './pages/schedule-page';
import { serverFetch } from './util/server';
import Schedule from './models/schedule';

const initApp = async (setPathsFunc: any) => {
    let data = await serverFetch("/paths");
    let paths = new Paths(data)
    setPathsFunc(paths);
};

const initSched = async (paths: Paths, setSchedFunc: any) => {
    let sched = await Schedule.asyncCreate('OTC', 'LOMBARD', paths)
    setSchedFunc(sched);
}

export default function App() {

    const [paths, setPaths] = useState(new Paths({}));
    const [schedule, setSchedule] = useState(Schedule.empty());

    useEffect(() => {
        initApp(setPaths);
    }, []);

    useEffect(() => {
        initSched(paths, setSchedule)
	}, [paths]);

    return (
        <React.Fragment>
            <Appbar.Header>
                <Appbar.Content title="Metrajay" />
                <Appbar.Action icon="folder-heart-outline" onPress={() => { }} />
                <Appbar.Action icon="sticker-plus-outline" onPress={() => { }} />
            </Appbar.Header>
            <SchedulePage schedule={schedule}/>
            <SelectPage paths={paths}/>
        </React.Fragment>
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
