angular.module('recipe')
	.component('recipeShow', {
		
		templateUrl : 'app/recipe/recipeShow/recipeShow.component.html',
		
		controller : function(recipeService,authService,$scope, $rootScope){
			
			var vm = this;
			
			vm.recipe = {}
			
			$scope.$on('recipeSelected', function(e,object){	
				if (!object.value && authService.getToken().id) {
					recipeService.showUserFavorites()
						.then(function(response){
							var favorites = response.data;
							favorites.forEach(function(fav,idx,arr){
								if (fav.recipe.id === vm.recipe.id) {
									vm.recipe.isFav = true;
								}
							})
					})

				}
				console.log(vm.recipe)
			})
			
			var checkLogin = function(){
				if(authService.getToken()) return;
				$location.path('/login')
			}
			
			vm.backButton = function(){
				$rootScope.$broadcast('recipeDaily', {
					value : vm.showDaily
				})
				vm.goBack();
			}
			
			vm.updateFavorites = function(recipe){
				if (recipe.isFav) {
					recipeService.addToFavorites(recipe)
						.then(function(response){
							console.log("favorite added")
						})
				}
				if (!recipe.isFav){
					recipeService.deleteFromFavorites(recipe)
					.then(function(response){
						if(response.data) console.log("favorite deleted")
					})
				}
			}
			$scope.displayNum = 0;
			$scope.increaseBy = function(num) {
				$scope.displayNum +=num;
			}
			
			vm.recipeRating = {};
			vm.recipe = {};
			vm.createRating = function(recipe,recipeRating){
				console.log(recipeRating)
				recipeRating.recipe = recipe;
				recipeService.createRating(recipeRating)
					.then(function(res){
						recipeService.showRecipe(vm.recipe)
							.then(function(resp){
								vm.recipe = resp.data;
								var total = 0;
								vm.recipe.rating.forEach(function(rat,idx,arr){
									total += rat.value
								})
								vm.recipe.avgRating = total/vm.recipe.rating.length;
								recipeService.showIngredients(vm.recipe)
									.then(function(resp){
										vm.recipe.recipeIngredients = resp.data;
										recipeService.showInstructions(vm.recipe)
											.then(function(resp){
												vm.recipe.instructions = resp.data;
												recipeService.showUserFavorites()
												.then(function(response){
													var favorites = response.data;
													favorites.forEach(function(fav,idx,arr){
														if (fav.recipe.id === vm.recipe.id) {
															vm.recipe.isFav = true;
														}
													})
											})
											})
									})
							})
					})
			}
			
			vm.isReadOnly = true;
			
			vm.max = 5;
		},
		
		controllerAs : 'vm',
		
		bindings : {
			recipe : '<',
			goBack : '&',
			showList : '='
		}
	})