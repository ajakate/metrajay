import { StatusBar } from 'expo-status-bar';
import { StyleSheet, Text, View } from 'react-native';
import React, { useEffect, useState, useRef, useReducer } from 'react';
import { Buffer } from "buffer";
import Paths from './models/paths';
import { ActivityIndicator, MD2Colors, Appbar } from 'react-native-paper';
import SelectPage from './pages/select-page';
import SchedulePage from './pages/schedule-page';
import { serverFetch } from './util/server';
import Schedule from './models/schedule';
import { createStackNavigator } from '@react-navigation/stack'
import { NavigationContainer } from '@react-navigation/native'
import { actionCreators, initialState, reducer } from './reducers/store';
import CustomContext from './reducers/context';
import CustomAppBar from './components/custom-app-bar';


const Stack = createStackNavigator();

const initApp = async (dispatch: any) => {
    let data = await serverFetch("/paths");
    dispatch(actionCreators.setPaths(new Paths(data)))
};

export default function App() {

    const [state, dispatch] = useReducer(reducer, initialState);

    const providerState = {
        state,
        dispatch
    }

    useEffect(() => {
        initApp(dispatch);
    }, []);

    return (
        <CustomContext.Provider value={providerState}>
            <NavigationContainer>
                <Stack.Navigator
                    initialRouteName="SelectPage"
                    screenOptions={{
                        header: CustomAppBar,
                    }}>
                    <Stack.Screen name="SelectPage" component={SelectPage} />
                    <Stack.Screen name="SchedulePage" component={SchedulePage} />
                </Stack.Navigator>
            </NavigationContainer>
        </CustomContext.Provider>
    )
}
