'use strict';

angular.module('testDataGenerator')
    .controller('TestDataGeneratorController', ['$http', '$interval', function ($http, $interval) {
        var self = this;

        self.status = {
            enabled: false,
            totalRuns: 0,
            intervalSeconds: 300,
            versions: {}
        };
        self.busy = false;

        function applyStatus(data) {
            self.status = data;
        }

        function refresh() {
            $http.get('api/gateway/test-data-generator/status').then(function (resp) {
                applyStatus(resp.data);
            });
        }

        self.toggle = function () {
            self.busy = true;
            $http.post('api/gateway/test-data-generator/toggle')
                .then(function (resp) {
                    applyStatus(resp.data);
                })
                .finally(function () {
                    self.busy = false;
                });
        };

        self.versionKeys = function () {
            return Object.keys(self.status.versions || {});
        };

        refresh();
        var poll = $interval(refresh, 5000);

        this.$onDestroy = function () {
            $interval.cancel(poll);
        };
    }]);
