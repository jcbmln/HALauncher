var handleThemeUpdate=function(a){a=a.data||a;var b=a.default_theme;window.externalApp.externalBus(JSON.stringify({type:"frontend/get_themes",themes:a.themes}));"default"===b?window.externalApp.themesUpdated(JSON.stringify({name:b})):window.externalApp.themesUpdated(JSON.stringify({name:b,styles:a.themes[b]}))};window.hassConnection.then(function(a){a=a.conn;a.sendMessagePromise({type:"frontend/get_themes"}).then(handleThemeUpdate);a.subscribeEvents(handleThemeUpdate,"themes_updated")});
