import { Text, ScrollView, View, Linking } from 'react-native';
import React, { useEffect, useState, useRef } from 'react';
import SearchableDropdown from 'react-native-searchable-dropdown';

export default function StationSelect(props: any) {

    const title = props.title;
    const stationList = props.stationList;
    const onSelect = props.onSelect;
    const selected = props.selected;
    const disabled = props.disabled;

    const disabledColor = disabled ? '#f7f7f7' : '#fff'

    return (
        <View>
            <Text>{title}</Text>
            <SearchableDropdown
                autosize={true}
                onItemSelect={(item: any) => onSelect(item)}
                containerStyle={{ padding: 5 }}
                itemStyle={{
                    padding: 10,
                    marginTop: 2,
                    backgroundColor: '#ddd',
                    borderColor: '#bbb',
                    borderWidth: 1,
                    borderRadius: 5,
                }}
                itemTextStyle={{ color: '#222' }}
                itemsContainerStyle={{ maxHeight: 140 }}
                items={stationList}
                defaultIndex={0}
                resetValue={false}
                textInputProps={
                    {
                        placeholder: selected.name,
                        underlineColorAndroid: "transparent",
                        editable: !disabled,
                        style: {
                            padding: 12,
                            borderWidth: 1,
                            borderColor: '#ccc',
                            borderRadius: 5,
                            backgroundColor: disabledColor
                        },
                    }
                }
                listProps={
                    {
                        nestedScrollEnabled: false,
                    }
                }
            />
        </View>
    )
}
