import { Appbar } from 'react-native-paper';
import { NavigationContext, useRoute } from '@react-navigation/native';
import React, { useEffect, useState, useRef, useReducer } from 'react';
import { observer } from 'mobx-react-lite';
import { useStore } from '../stores/store';
import { StyleSheet, Text, View } from 'react-native';

// TODO: fix wonky scheduleInFavorite stuff... :(
const options = {
    'FavoritesScreen': {
        title: "My Routes",
        barButtons: (navigation, _) => (
            <Appbar.Action icon="sticker-plus-outline" onPress={() => { navigation.navigate('SelectScreen') }} />
        )
    },
    'SelectScreen': {
        title: "Find a Schedule",
        barButtons: (navigation, _) => (
            <Appbar.Action icon="folder-heart-outline" onPress={() => { navigation.navigate('FavoritesScreen') }} />
        )
    },
    'ScheduleScreen': {
        title: "View Schedule",
        barButtons: (navigation, { scheduleInFavorite, isScheduleLoading, addCurrentFavorite, removeCurrentFavorite, loadingFavorites }) => {

            const Disp = observer((props) => {
                return ((!props.inFavorite()) ? (
                    <Appbar.Action icon="cards-heart-outline" onPress={() => { addCurrentFavorite() }} />
                ) : (
                    <Appbar.Action icon="cards-heart" onPress={() => { removeCurrentFavorite() }} />
                ))
            })

            return (
                <React.Fragment>
                    <Appbar.Action icon="folder-heart-outline" onPress={() => { navigation.navigate('FavoritesScreen') }} />
                    <Appbar.Action icon="sticker-plus-outline" onPress={() => { navigation.navigate('SelectScreen') }} />
                    {
                        isScheduleLoading() ? (
                            <View></View>
                        ) : (loadingFavorites ? (
                            <Appbar.Action icon="reload" onPress={() => { }} />
                        ) : (<Disp inFavorite={scheduleInFavorite} />))
                    }
                </React.Fragment>
            )
        }
    },
}

const CustomAppBar = observer((props) => {

    const navigation = React.useContext(NavigationContext);
    const store = useStore();

    const back = props.back
    const route = useRoute();

    return (
        <Appbar.Header>
            {back ? <Appbar.BackAction onPress={navigation.goBack} /> : null}
            <Appbar.Content title={options[route.name].title} />
            {options[route.name].barButtons(navigation, store)}
        </Appbar.Header>
    );
});

export default CustomAppBar;
