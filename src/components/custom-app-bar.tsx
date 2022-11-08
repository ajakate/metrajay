import { Appbar } from 'react-native-paper';

export default function CustomAppBar() {
  return (
    <Appbar.Header>
        <Appbar.Content title="Station2Station"/>
        <Appbar.Action icon="folder-heart-outline" onPress={() => { }} />
        <Appbar.Action icon="sticker-plus-outline" onPress={() => { }} />
    </Appbar.Header>
  );
};
