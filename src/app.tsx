import { NavigationContainer } from '@react-navigation/native';
import { createStackNavigator } from '@react-navigation/stack';
import { observer } from 'mobx-react-lite';
import React, { useEffect } from 'react';
import CustomAppBar from './components/custom-app-bar';
import LoadingWrapper from './components/loading-wrapper';
import Paths from './models/paths';
import FavoritesPage from './pages/favorites-page';
import SchedulePage from './pages/schedule-page';
import SelectPage from './pages/select-page';
import { useStore } from './stores/store';
import { serverFetch } from './util/server';

const Stack = createStackNavigator();

// TODO: move this logic
const initApp = async (setPaths: any, loadFavorites:any) => {
    let data = await serverFetch("/paths");
    setPaths(new Paths(data))
    loadFavorites()
};

const FavoritesScreen = observer(() => {

    const { loadingFavorites } = useStore();

    return (
        <LoadingWrapper loading={loadingFavorites}>
            <FavoritesPage/>
        </LoadingWrapper>
    )
});

const ScheduleScreen = observer(() => {

    const { isScheduleLoading } = useStore();

    return (
        <LoadingWrapper loading={isScheduleLoading()}>
            <SchedulePage/>
        </LoadingWrapper>
    )
});

const SelectScreen = observer(() => {

    const { isPathLoading } = useStore();

    return (
        <LoadingWrapper loading={isPathLoading()}>
            <SelectPage/>
        </LoadingWrapper>
    )
});

const Metrajay = observer(() => {

    const { setPaths, loadFavorites } = useStore();

    useEffect(() => {
        initApp(setPaths, loadFavorites);
    }, []);

    return (
        <NavigationContainer>
            <Stack.Navigator
                initialRouteName="FavoritesScreen"
                screenOptions={{
                    header: props => <CustomAppBar {...props} />,
                }}>
                <Stack.Screen name="SelectScreen" component={SelectScreen} />
                <Stack.Screen name="ScheduleScreen" component={ScheduleScreen} />
                <Stack.Screen name="FavoritesScreen" component={FavoritesScreen} />
            </Stack.Navigator>
        </NavigationContainer>
    )
})

export default Metrajay;
