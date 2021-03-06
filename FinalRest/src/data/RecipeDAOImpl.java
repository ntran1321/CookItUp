package data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;

import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import entities.Favorite;
import entities.Ingredient;
import entities.IngredientDTO;
import entities.Instruction;
import entities.InstructionDTO;
import entities.Rating;
import entities.RatingDTO;
import entities.Recipe;
import entities.RecipeDTO;
import entities.RecipeIngredient;
import entities.Tag;
import entities.User;

@Transactional
public class RecipeDAOImpl implements RecipeDAO {

	@PersistenceContext
	private EntityManager em;

	@Override
	public Set<Recipe> index(String json) throws IOException {
		ObjectMapper mapper = new ObjectMapper();

		List<Ingredient> ingredients = mapper.readValue(json, new TypeReference<List<Ingredient>>() {
		});

		String ingredientQuery = "SELECT i FROM Ingredient i WHERE i.name LIKE :name";
		String recipeQuery = "SELECT r FROM Recipe r JOIN FETCH r.tags WHERE";

		List<Ingredient> managedIngs = new ArrayList<Ingredient>();
		for (Ingredient ingd : ingredients) {
			List<Ingredient> managedResults;
			try {
				managedResults = em.createQuery(ingredientQuery, Ingredient.class).setParameter("name", "%"+ingd.getName()+"%")
						.getResultList();
				
				if (managedResults.size() > 0) {
					for (Ingredient i : managedResults) {
						managedIngs.add(i);
					}
					continue;
				}
				
			} catch (Exception e) {
				e.printStackTrace();
				continue;
			}
			
		}
		for (int i = 0; i < managedIngs.size(); i++) {
			if (i == managedIngs.size() - 1) {
				recipeQuery += " :ing" + i + " MEMBER OF r.ingredients";
				break;
			}
			recipeQuery += " :ing" + i + " MEMBER OF r.ingredients OR";
		}
		if (managedIngs.size() == 0) {
			return new HashSet<Recipe>();
		}
		System.out.println(recipeQuery);
		TypedQuery<Recipe> recipes = em.createQuery(recipeQuery, Recipe.class);
		for (int i = 0; i < managedIngs.size(); i++) {
			recipes.setParameter("ing" + i, managedIngs.get(i));
		}
		List<Recipe> r = recipes.getResultList();
		for (Recipe recipe : r) {
			System.out.println(recipe.getTitle());
		}
		return new HashSet<Recipe>(r);
	}

	@Override
	public Recipe show(int rid) {
		Recipe r = em.createQuery("SELECT r FROM Recipe r "
				+ "JOIN FETCH r.tags WHERE r.id="+rid, Recipe.class).getSingleResult();
		return r;
	}

	@Override
	public Recipe createRecipe(int uid, String recipeJson) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			RecipeDTO recipeDTO = mapper.readValue(recipeJson, RecipeDTO.class);
			Recipe r = new Recipe();
			r.setTitle(recipeDTO.getTitle());
			r.setImgUrl(recipeDTO.getImgUrl());

			List<Tag> recipeDtoTags = recipeDTO.getTags();
			if(recipeDtoTags.size()==0) {
				Tag tag = new Tag();
				tag.setName("User-submitted");
				List<Tag> tags = new ArrayList<>();
				tags.add(tag);
				recipeDtoTags = tags;
				recipeDtoTags.add(tag);
			}
			for (Tag tag : recipeDtoTags) {
				List<Tag> managedTags = em.createQuery("SELECT t FROM Tag t WHERE t.name=:name", Tag.class)
						.setParameter("name", tag.getName())
						.getResultList();
				if(managedTags.size() > 0){
					List<Tag> tags = new ArrayList<>();
					tags.add(managedTags.get(0));
					r.setTags(tags);
				}
				else{
					Tag t = new Tag();
					t.setName(tag.getName());
					List<Tag> tags = new ArrayList<>();
					tags.add(t);
					r.setTags(tags);
					em.persist(t);
				}
			}
			em.persist(r);
			em.flush();

			String qry = "Select i from Ingredient i";
			List<Ingredient> managedIngs = new ArrayList<>();
			managedIngs = em.createQuery(qry, Ingredient.class).getResultList();

			for (IngredientDTO i : recipeDTO.getIngredients()) {
				Ingredient managedIng = null;
				boolean exists = false;

				for (Ingredient ingredient : managedIngs) {
					if (ingredient.getName().toLowerCase().equals(i.getName().toLowerCase())) {
						exists = true;
						managedIng = ingredient;
					}
				}

				if (exists) {
					RecipeIngredient ri = new RecipeIngredient();
					ri.setIngredient(managedIng);
					ri.setRecipe(r);
					ri.setQuantity(i.getQuantity());
					em.persist(ri);
					em.flush();
				} else {
					Ingredient newIng = new Ingredient();
					newIng.setName(i.getName());
					em.persist(newIng);
					em.flush();

					RecipeIngredient ri = new RecipeIngredient();
					ri.setIngredient(newIng);
					ri.setRecipe(r);
					ri.setQuantity(i.getQuantity());
					em.persist(ri);
					em.flush();
				}

			}

			List<InstructionDTO> instructions = recipeDTO.getInstructions();
			for (InstructionDTO instr : instructions) {
				Instruction newInstr = new Instruction();
				newInstr.setStepNumber(instr.getStepNumber());
				newInstr.setText(instr.getText());
				newInstr.setRecipe(r);
				em.persist(newInstr);
				em.flush();
			}
			return r;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Rating createRating(int uid, String ratingJson) {
		System.out.println(ratingJson);
		ObjectMapper mapper = new ObjectMapper();
		try {
			RatingDTO ratingDto = mapper.readValue(ratingJson, RatingDTO.class);
			Rating r = new Rating();
			r.setRecipe(ratingDto.getRecipe());
			r.setUser(em.find(User.class, uid));
			r.setValue(ratingDto.getValue());
			r.setReview(ratingDto.getReview());
			em.persist(r);
			em.flush();

			return r;
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Recipe update(int uid, int rid, String recipeJson) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Boolean destroy(int uid, int rid) {
		// TODO Auto-generated method stub
		return null;
	}

	public Recipe addToFavorite(int uid, int rid) {
		// TODO Auto-generated method stub
		Favorite fave = new Favorite();
		fave.setRecipe(em.createQuery("SELECT r FROM Recipe r JOIN FETCH " + "r.tags WHERE r.id =" + rid, Recipe.class)
				.getSingleResult());
		// fave.setRecipe(em.find(Recipe.class, rid));
		fave.setUser(em.find(User.class, uid));
		em.persist(fave);
		em.flush();
		return fave.getRecipe();
	}

	@Override
	public Set<Favorite> showFavorite(int uid) {
		String favoriteQ = "SELECT f FROM Favorite f JOIN FETCH f.recipe r "
				+ "JOIN FETCH r.tags WHERE f.user.id = :uid";
		return new HashSet<Favorite>(
				em.createQuery(favoriteQ, Favorite.class).setParameter("uid", uid).getResultList());
	}

	public Boolean destroyFave(int uid, int rid) {

		try {
			String query = "SELECT f FROM Favorite f WHERE f.user.id = :uid AND f.recipe.id = :rid";
			Favorite f = em.createQuery(query, Favorite.class).setParameter("uid", uid).setParameter("rid", rid)
					.getSingleResult();
			em.remove(f);
			return true;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return false;
		}

	}

	@Override
	public Set<RecipeIngredient> showIngredients(int rid) {
		String query = "SELECT ring FROM RecipeIngredient ring WHERE ring.recipe.id = :rid";
		return new HashSet<RecipeIngredient>(
				em.createQuery(query, RecipeIngredient.class).setParameter("rid", rid).getResultList());
	}

	@Override
	public Set<Instruction> showInstructions(int rid) {
		String query = "SELECT i FROM Instruction i WHERE i.recipe.id = :rid";
		return new HashSet<Instruction>(
				em.createQuery(query, Instruction.class).setParameter("rid", rid).getResultList());
	}

	@Override
	public Rating addRating(int uid, int rid, String jsonRating) {
		ObjectMapper mapper = new ObjectMapper();
		try {
			Rating rating = mapper.readValue(jsonRating, Rating.class);
			rating.setUser(em.find(User.class, uid));
			rating.setRecipe(em.find(Recipe.class, rid));
			em.persist(rating);
			em.flush();
			return rating;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	@Override
	public Set<Tag> showTags() {
		String query = "SELECT t from Tag t";
		return new HashSet<>(em.createQuery(query, Tag.class).getResultList());
	}

	@Override
	public Set<Recipe> showFilteredRecipes(int tid) {
		String query = "SELECT r FROM recipe r JOIN recipe_tag rt ON r.id = rt.recipe_id"
				+ "JOIN tag t ON rt.tag_id = t.id WHERE t.id = :tid";
		return new HashSet<>(em.createQuery(query, Recipe.class).setParameter("tid", tid).getResultList());
	}

	@Override
	public Recipe showRandomRecipe() {
		String query = "SELECT r FROM Recipe r JOIN FETCH r.tags";
		List<Recipe> rec = em.createQuery(query, Recipe.class).getResultList();
		Collections.shuffle(rec);
		return rec.get(0);
	}

	@Override
	public Set<Ingredient> showIngred(int id) {
		System.out.println(id);
		String query = "SELECT i FROM Ingredient i JOIN recipe_ingredient ri ON ri.ingredient.id = i.id WHERE ri.recipe.id = :id";
		System.out
				.println(new HashSet<>(em.createQuery(query, Ingredient.class).setParameter("id", id).getResultList()));
		return new HashSet<>(em.createQuery(query, Ingredient.class).setParameter("id", id).getResultList());
	}
	@Override
	public Set<Ingredient> indexIngred() {

		String query = "SELECT i FROM Ingredient i";
		return new HashSet<>(em.createQuery(query, Ingredient.class).getResultList());
	}
}
