import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { observer } from 'mobx-react-lite';
import React, { useEffect } from 'react';
import CustomAppBar from './components/custom-app-bar';
import Paths from './models/paths';
import ScheduleScreen from './screens/schedule-screen';
import SelectScreen from './screens/select-screen';
import { useStore } from './stores/store';
import { serverFetch } from './util/server';

const Stack = createStackNavigator();

// TODO: move this logic
const initApp = async (setPaths: any) => {
    let data = await serverFetch("/paths");
    setPaths(new Paths(data))
};

const Metrajay = observer(() => {

    const { setPaths } = useStore();

    useEffect(() => {
        initApp(setPaths);
    }, []);

    return (
        <NavigationContainer>
            <Stack.Navigator
                initialRouteName="SelectScreen"
                screenOptions={{
                    header: CustomAppBar,
                }}>
                <Stack.Screen name="SelectScreen" component={SelectScreen} />
                <Stack.Screen name="ScheduleScreen" component={ScheduleScreen} />
            </Stack.Navigator>
        </NavigationContainer>
    )
})

export default Metrajay;
